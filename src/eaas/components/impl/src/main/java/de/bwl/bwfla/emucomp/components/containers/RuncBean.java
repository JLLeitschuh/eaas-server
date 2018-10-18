/*
 * This file is part of the Emulation-as-a-Service framework.
 *
 * The Emulation-as-a-Service framework is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * The Emulation-as-a-Service framework is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Emulation-as-a-Software framework.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package de.bwl.bwfla.emucomp.components.containers;

import de.bwl.bwfla.common.datatypes.EmuCompState;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.common.utils.net.ConfigKey;
import de.bwl.bwfla.common.utils.net.PortRangeProvider;
import de.bwl.bwfla.emucomp.api.ContainerConfiguration;
import de.bwl.bwfla.emucomp.api.EmulatorUtils;
import de.bwl.bwfla.emucomp.api.OciContainerConfiguration;
import de.bwl.bwfla.emucomp.control.connectors.XpraConnector;
import de.bwl.bwfla.emucomp.xpra.XpraUtils;
import org.apache.tamaya.inject.api.Config;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Paths;


public class RuncBean extends ContainerBean
{
	@Inject
	@Config("components.binary.runc")
	private String runc = null;
	int port = -1;
	@Inject
	@ConfigKey("components.xpra.ports")
	protected PortRangeProvider.Port listenPort;

	boolean isGui = false;

	private DeprecatedProcessRunner xpraRunner = new DeprecatedProcessRunner();

	@Override
	protected void prepare() throws Exception
	{
		final String cid = this.getContainerId();
		final String workdir = this.getWorkingDir().toString();
		final OciContainerConfiguration config = (OciContainerConfiguration) super.config;

		// Resolve and mount runc-specific bindings
		{
			final EmulatorUtils.XmountOutputFormat format = EmulatorUtils.XmountOutputFormat.RAW;
			bindings.mount(config.getRootFilesystem(), this.getBindingsDir(), format);
		}

		// Generate container's config
		{
			final String conConfigPath = Paths.get(workdir, "config.json").toString();
			final String rootfs = bindings.lookup(config.getRootFilesystem());

			final DeprecatedProcessRunner cgen = new DeprecatedProcessRunner();
			cgen.setCommand("emucon-cgen");
			cgen.addArguments("--rootfs", rootfs);
			cgen.addArguments("--output", conConfigPath);
			if (this.isUserNamespaceEnabled()) {
				cgen.addArguments("--user", this.getContainerRuntimeUser());
				cgen.addArguments("--group", this.getContainerRuntimeGroup());
			}

			// Container's inputs
			if (config.hasInputs()) {
				for (ContainerConfiguration.Input input : config.getInputs()) {
					final String src = bindings.lookup(input.getBinding());
					final String dst = input.getDestination();
					if(src!= null && dst != null) {
						cgen.addArgument("--mount");
						cgen.addArgument(src, ":", dst, ":bind:ro");
					}
				}
			}

			// Container's output directory mount
			if (config.hasOutputPath()) {
				final String src = this.getOutputDir().toString();
				final String dst = config.getOutputPath();
				cgen.addArgument("--mount");
				cgen.addArgument(src, ":", dst, ":bind:rw");
			}

			if(config.isGui()) {
				isGui = true;
				cgen.addArguments("--env", "DISPLAY=:" + getPort());
				cgen.addArgument("--mount");
				cgen.addArgument("/tmp/.X11-unix/", ":", "/tmp/.X11-unix/", ":bind:rw");
			}
			// Add environment variables
			if(config.getProcess().getEnvironmentVariables() != null) {
				for (String env : config.getProcess().getEnvironmentVariables())
					cgen.addArguments("--env", env);
			}



			// Add emulator's command
			cgen.addArgument("--");
			for (String arg : config.getProcess().getArguments())
				cgen.addArgument(arg);

			cgen.setLogger(LOG);
			if (!cgen.execute()) {
				conBeanState.update(ContainerState.FAILED);

				final String message = "Generating container's config failed!";
				LOG.warning(message);
				throw new BWFLAException(message);
			}
		}

		conRunner.setCommand("sudo");
		conRunner.addArguments(runc, "--debug", "run", cid);
		conRunner.setWorkingDirectory(this.getWorkingDir());
	}



	@Override
	public void start() throws BWFLAException {

		if (isGui)
			try {

				port = getPort();
				XpraUtils.startXpraSession(xpraRunner, port, LOG);

				if (!XpraUtils.waitUntilReady(port, 50000)) {
					conBeanState.update(ContainerState.FAILED);
					throw new BWFLAException("Connecting to Xpra server failed!");
				}

				DeprecatedProcessRunner runner = new DeprecatedProcessRunner();
				runner.setCommand("xhost");
				runner.addArgument("+local:root");
				runner.addEnvVariable("DISPLAY", ":" + getPort());
				runner.execute();
				LOG.info("Xpra server started in process " + xpraRunner.getProcessId());

				final Thread xpraObserver = workerThreadFactory.newThread(() -> {
							xpraRunner.waitUntilFinished();
							// cleanup will be performed later by EmulatorBean.destroy()
							synchronized (conBeanState) {
								final ContainerState curstate = conBeanState.get();
								if (curstate == ContainerState.RUNNING) {
									LOG.info("Xpra server stopped unexpectedly or user stopped/left the session");
								}
							}
						}
				);
				xpraObserver.start();

				this.addControlConnector(new XpraConnector(port));
			} catch (IOException e) {
				throw new BWFLAException("xpra initiation failed \n" + e.getMessage());
			}


		super.start();
	}

	@Override
	public void stop() throws BWFLAException {
		if (xpraRunner.isProcessRunning())
			EmulatorUtils.stopXpraServer(xpraRunner);
		super.stop();
	}

	@Override
	public void destroy(){
		if (port != -1)
			listenPort.release();
		super.destroy();
	}



	public int getPort() throws IOException {
		if (port == -1)
			this.port = listenPort.get();
		return port;
	}
}