/*
 * Copyright (C) 2015 Jtalk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.jtalk.socketconnector;

import me.jtalk.socketconnector.api.TCPConnection;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Optional;
import javax.resource.ResourceException;

import me.jtalk.socketconnector.utils.ConnectorLogger;
import me.jtalk.socketconnector.utils.NamedIdObject;
import static me.jtalk.socketconnector.utils.LazyLogger.lazyTrace;

public class TCPConnectionImpl implements TCPConnection, NamedIdObject {
	private final ConnectorLogger log = new ConnectorLogger(this.getName());


	private static final long NO_ID = -1L;
	private final String className = this.getClass().getName();
	private volatile ManagedTCPConnectionProxy owner;

	public TCPConnectionImpl(ManagedTCPConnectionProxy owner) {
		this.owner = owner;
		lazyTrace(log, "TCP Connection created: {}" + className);
	}

	@Override
	public long getId() throws ResourceException {
		return getIdInternal()
				.orElseThrow(() -> new ResourceException("Connection is detached"));
	}

	@Override
	public void send(ByteBuffer message) throws ResourceException {
		ManagedTCPConnectionProxy local = this.owner;
		if (local == null) {
			lazyTrace(log, "Data will not be sent through {}: the connection is detached" + className);
			throw new ResourceException("Connection is detached");
		}
		lazyTrace(log, "Data will be sent through {}" + className);
		local.send(message);
	}

	@Override
	public void disconnect() throws ResourceException {
		ManagedTCPConnectionProxy local = this.owner;
		if (local != null) {
			lazyTrace(log, "Disconnection request satisfied for {}" + className);
			local.disconnect();
		}
		lazyTrace(log, "Disconnection request rejected for {}: the connection is detached" + className);
	}

	@Override
	public void close() throws ResourceException {
		ManagedTCPConnectionProxy local = this.owner;
		if (local != null) {
			lazyTrace(log, "Closing connection {}" + className);
			local.requestCleanup();
		}
		lazyTrace(log, "Closing request denied for {}: the connection is detached" + className);
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}: [''{1}'']", super.toString(), getIdInternal().orElse(NO_ID));
	}

	void reassign(ManagedTCPConnectionProxy newOwner) {
		lazyTrace(log, "Reassigning {}: the new ID is {}" + className);
		this.owner = newOwner;
	}

	void invalidate() {
		lazyTrace(log, "Invalidating {}" + className);
		this.owner = null;
	}



	private Optional<Long> getIdInternal() {
		ManagedTCPConnectionProxy local = this.owner;
		if (local == null) {
			return Optional.empty();
		}
		long result = local.getId();
		return Optional.of(result);
	}

}
