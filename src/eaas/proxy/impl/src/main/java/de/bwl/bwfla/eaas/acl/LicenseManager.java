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

package de.bwl.bwfla.eaas.acl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import de.bwl.bwfla.objectarchive.util.ObjectArchiveHelper;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;

import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.emucomp.api.ComponentConfiguration;
import de.bwl.bwfla.emucomp.api.MachineConfiguration;
import de.bwl.bwfla.softwarearchive.util.SoftwareArchiveHelper;

public abstract class LicenseManager implements IAccessControlList {
    @Inject
    Logger log;

    @Inject
    protected AclConfig aclConfig;

    protected Map<String, SeatCounter> seats = new ConcurrentHashMap<String, SeatCounter>();
    protected Map<UUID, List<String>> allocations = new ConcurrentHashMap<UUID, List<String>>();

    protected void releaseSeat(String id)
    {
        SeatCounter counter = seats.get(id);
        if (counter == null)
            return;
        
        counter.decrement();
    }

    /**
     * Allocates a license seat for the given id or throws an
     * OutOfSeatsException if already allocated and the number of available seats is reached.
     *
     * If this method does not throw, it is guaranteed that there was one free
     * software license for the given softwareId and that the number of free
     * licenses was decremented successfully.
     *
     * @param id
     * @param max
     * @throws OutOfSeatsException
     */
    protected void allocate(String id, int max) throws OutOfSeatsException
    {
        try {
            SeatCounter counter = seats.computeIfAbsent(id, sid -> new SeatCounter(max));

            if (!counter.increment()) {
                throw new OutOfSeatsException("Maximum number of software licenses reached for software id \"" + id + "\"");
            }
        } catch (RuntimeException e) {
            if (e.getCause() instanceof OutOfSeatsException) {
                throw (OutOfSeatsException)e.getCause();
            }
            log.severe("Internal runtime Error: " + e.getMessage());
            throw e;
        }
    }
}

class SeatCounter
{
    private final AtomicInteger curNumSeats;
    private final int maxNumSeats;
    
    public SeatCounter(int maxnum)
    {
        this.curNumSeats = new AtomicInteger(0);
        this.maxNumSeats = maxnum;
    }
    
    public boolean increment()
    {
        int newnum = curNumSeats.incrementAndGet();
        if (newnum > maxNumSeats) {
            // Max number of seats reached!
            curNumSeats.decrementAndGet();
            return false;
        }
        
        return true;
    }
    
    public int decrement()
    {
        return curNumSeats.decrementAndGet();
    }
}