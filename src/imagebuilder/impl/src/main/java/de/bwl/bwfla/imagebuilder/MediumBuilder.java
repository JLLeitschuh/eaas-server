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

package de.bwl.bwfla.imagebuilder;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.DeprecatedProcessRunner;
import de.bwl.bwfla.emucomp.api.EmulatorUtils;
import de.bwl.bwfla.emucomp.api.XmountOptions;
import de.bwl.bwfla.imagebuilder.api.ImageContentDescription;
import de.bwl.bwfla.imagebuilder.api.ImageDescription;
import org.apache.commons.io.FileUtils;

import javax.activation.DataHandler;
import javax.activation.URLDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


public abstract class MediumBuilder
{
	protected final Logger log = Logger.getLogger(this.getClass().getName());

	public abstract ImageHandle execute(Path workdir, ImageDescription description) throws BWFLAException;


	/* ==================== Internal Helpers ==================== */

	public static void delete(Path start, Logger log)
	{
		// Delete file or a directory recursively
		try (final Stream<Path> stream = Files.walk(start)) {
			final Consumer<Path> deleter = (path) -> {
				try {
					Files.delete(path);
				}
				catch (Exception error) {
					final String message = "Deleting '" + path.toString() + "' failed! ("
							+ error.getClass().getName() + ": " + error.getMessage() + ")";

					log.warning(message);
				}
			};

			stream.sorted(Comparator.reverseOrder())
					.forEach(deleter);
		}
		catch (Exception error) {
			String message = "Deleting '" + start.toString() + "' failed!\n";
			log.log(Level.WARNING, message, error);
		}
	}

	public static void unmount(Path path, Logger log)
	{
		try {
			EmulatorUtils.unmount(path, log);
		}
		catch (Exception error) {
			log.log(Level.WARNING, "Unmounting '" + path.toString() + "' failed!\n", error);
		}
	}

	public static Path remount(Path device, Path mountpoint, XmountOptions options, Logger log)
			throws BWFLAException, IOException
	{
		MediumBuilder.sync(mountpoint, log);
		EmulatorUtils.unmount(mountpoint, log);
		return EmulatorUtils.xmount(device.toString(), mountpoint, options, log);
	}

	public static void copy(List<ImageContentDescription> entries, Path dstdir, Path workdir, Logger log) throws IOException, BWFLAException {
		for (ImageContentDescription entry : entries) {
			DataHandler handler;

			if (entry.getURL() != null) {
				handler = new DataHandler(new URLDataSource(entry.getURL()));
			} else {
				handler = entry.getData();
			}

			switch (entry.getAction()) {
				case COPY:

					if (entry.getName() == null || entry.getName().isEmpty())
						throw new BWFLAException("entry with action COPY must have a valid name");

					Path outpath = dstdir.resolve(entry.getName());
					// FIXME: file names must be unique!
					if (outpath.toFile().exists())
						outpath = outpath.getParent().resolve(outpath.getFileName() + "-" + UUID.randomUUID());
					Files.copy(handler.getInputStream(), outpath);
					break;

				case EXTRACT:
					// Extract archive directly to destination!
					FilesExtractor.extract(handler, dstdir, entry.getArchiveFormat(), workdir, log);
					break;
			}
		}
	}

	public static void sync(Path path, Logger log)
	{
		final DeprecatedProcessRunner process = new DeprecatedProcessRunner("sync");
		process.setLogger(log);
		if (!process.execute())
			log.warning("Syncing filesystem for '" + path.toString() + "' failed!");
	}
}