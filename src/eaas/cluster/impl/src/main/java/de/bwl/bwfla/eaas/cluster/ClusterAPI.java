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

package de.bwl.bwfla.eaas.cluster;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import de.bwl.bwfla.common.logging.PrefixLogger;
import de.bwl.bwfla.common.logging.PrefixLoggerContext;
import de.bwl.bwfla.eaas.cluster.dump.DumpConfig;
import de.bwl.bwfla.eaas.cluster.dump.DumpFlags;
import de.bwl.bwfla.eaas.cluster.dump.DumpHelpers;


@Path("api/v1/clusters")
public class ClusterAPI
{
	private static final int NUM_BASE_SEGMENTS = 3;
	
	/* Supported Http-Headers **/
	private static class Headers
	{
		private static final String ADMIN_ACCESS_TOKEN = "X-Admin-Access-Token";
	}
	
	
	private PrefixLogger log;

	@Context
	private UriInfo uri;
	
	@Inject
	private IClusterManager clustermgr;


	/* ========== Public API ========== */

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listClusters(@HeaderParam(Headers.ADMIN_ACCESS_TOKEN) String token)
	{
		// Currently max. 1 is supported!
		
		final Function<JsonGenerator, Status> handler = (json) -> {
			json.writeStartArray();
			if (clustermgr != null) {
				ClusterAPI.authorize(clustermgr, token);
				json.write(clustermgr.getName());
			}

			json.writeEnd();
			return Status.OK;
		};

		return this.execute(handler, JSON_RESPONSE_CAPACITY);
	}
	
	@GET
	@Path("/{cluster_name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getClusterResource(
			@PathParam("cluster_name") String name,
			@HeaderParam(Headers.ADMIN_ACCESS_TOKEN) String token)
	{
		return this.execute(name, token, JSON_RESPONSE_CAPACITY);
	}
	
	@GET
	@Path("/{cluster_name}/{subres:.*}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getClusterSubResource(
			@PathParam("cluster_name") String name,
			@HeaderParam(Headers.ADMIN_ACCESS_TOKEN) String token)
	{
		return this.execute(name, token, JSON_RESPONSE_CAPACITY);
	}
	
	
	/* ========== Internal Helpers ========== */

	private static final int JSON_RESPONSE_CAPACITY = 4 * 1024;
	
	/** Constructor for CDI */
	public ClusterAPI()
	{
		// Empty!
	}

	@PostConstruct
	public void initialize()
	{
		PrefixLoggerContext logContext = new PrefixLoggerContext();
		logContext.add("CM", clustermgr.getName());

		this.log = new PrefixLogger(this.getClass().getName(), logContext);
	}
	
	private List<PathSegment> skipPathSegments(int num)
	{
		final List<PathSegment> segments = uri.getPathSegments();
		return segments.subList(NUM_BASE_SEGMENTS + num, segments.size());
	}

	private Response execute(Function<JsonGenerator, Status> handler, int capacity)
	{
		final StringWriter buffer = new StringWriter(capacity);
		try (JsonGenerator json = ClusterAPI.newJsonGenerator(buffer, true)) {
			final Status status = handler.apply(json);
			json.flush();
			
			return ClusterAPI.newResponse(status, buffer);
		}
		catch (Throwable error) {
			final String url = uri.getAbsolutePath().toString();
			log.log(Level.WARNING, "Executing handler for URL '" + url + "' failed!\n", error);
			if (error instanceof WebApplicationException) 
				return ((WebApplicationException) error).getResponse();
			else return ClusterAPI.newErrorResponse(error);
		}
	}
	
	private Response execute(String clusterName, String token, int capacity)
	{
		final Function<JsonGenerator, Status> handler = (json) -> {
			final IClusterManager cluster = this.findClusterManager(clusterName);
			ClusterAPI.authorize(clustermgr, token);
			
			DumpConfig dconf = new DumpConfig(this.skipPathSegments(1), uri.getQueryParameters());
			cluster.dump(json, dconf, DumpFlags.TIMESTAMP | DumpFlags.RESOURCE_TYPE);
			return Status.OK;
		};
		
		return this.execute(handler, capacity);
	}
	
	private IClusterManager findClusterManager(String name)
	{
		if (clustermgr == null || !clustermgr.getName().contentEquals(name)) {
			String message = "Cluster manager '" + name + "' was not found!";
			throw new NotFoundException(message);
		}
		
		return clustermgr;
	}
	
	private static void authorize(IClusterManager cluster, String token) throws NotAuthorizedException
	{
		if (token != null && cluster.checkAccessToken(token))
			return;
		
		throw new NotAuthorizedException(ClusterAPI.newUnauthorizedResponse(token));
	}

	private static JsonGenerator newJsonGenerator(Writer writer, boolean pretty)
	{
		if (!pretty)
			return Json.createGenerator(writer);

		final Map<String, Boolean> config = new HashMap<String, Boolean>();
		config.put(JsonGenerator.PRETTY_PRINTING, pretty);
		return Json.createGeneratorFactory(config)
				.createGenerator(writer);
	}
	
	private static Response newErrorResponse(Throwable error)
	{
		final JsonObjectBuilder json = Json.createObjectBuilder();
		DumpHelpers.addResourceTimestamp(json);
		DumpHelpers.addResourceType(json, "InternalServerError");
		json.add("error_type", error.getClass().getName())
			.add("error_message", error.getMessage());
		
		return ClusterAPI.newResponse(Status.INTERNAL_SERVER_ERROR, json.build().toString());
	}
	
	private static Response newUnauthorizedResponse(String token)
	{
		final JsonObjectBuilder json = Json.createObjectBuilder();
		DumpHelpers.addResourceTimestamp(json);
		DumpHelpers.addResourceType(json, "UnauthorizedError");
		
		String errormsg = (token == null) ? "Access token is missing!" : "Access token is invalid!";
		json.add("error_message", errormsg);
		
		return ClusterAPI.newResponse(Status.UNAUTHORIZED, json.build().toString());
	}
	
	private static Response newResponse(Status status, StringWriter message)
	{
		return ClusterAPI.newResponse(status, message.toString());
	}
	
	private static Response newResponse(Status status, String message)
	{
		return Response.status(status)
				.entity(message)
				.build();
	}
}
