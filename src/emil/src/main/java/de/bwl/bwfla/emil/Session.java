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

package de.bwl.bwfla.emil;

import de.bwl.bwfla.emil.datatypes.SessionResource;

import java.util.List;


public class Session
{
	private final String id;
	private final List<SessionResource> resources;
	private long expirationTimestamp;

	public Session(String id, List<SessionResource> resources)
	{
		this(id, resources, Long.MAX_VALUE);
	}

	public Session(String id, List<SessionResource> resources, long expirationTimestamp)
	{
		this.id = id;
		this.resources = resources;
		this.expirationTimestamp = expirationTimestamp;
	}

	public String id()
	{
		return id;
	}

	public List<SessionResource> resources()
	{
		return resources;
	}

	public long getExpirationTimestamp()
	{
		return expirationTimestamp;
	}

	public void setExpirationTimestamp(long timestamp)
	{
		this.expirationTimestamp = timestamp;
	}
}