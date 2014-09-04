/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.services.jaxrs.peoplesample;

import org.apache.cxf.rs.security.cors.CorsHeaderConstants;
import org.wso2.am.integration.services.jaxrs.peoplesample.bean.Person;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Collection;

@Path("/people")
public class PeopleRestService {
	@Inject
	private PeopleService peopleService;

	public void PeopleRestService() {
		peopleService.setInitPeople();
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	public Collection<Person> getPeople(@QueryParam("page") @DefaultValue("1") final int page) {
		return peopleService.getPeople(page, 5);
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@PUT
	public Person addPerson(@FormParam("email") final String email) {
		return peopleService.addPerson(email);
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/{email}")
	@GET
	public Person getPeople(@PathParam("email") final String email) {
		return peopleService.getByEmail(email);
	}

/*
    @Produces( { MediaType.APPLICATION_FORM_URLENCODED  } )
    @POST
    public Response addPerson( @Context final UriInfo uriInfo,
                               @FormParam( "email" ) final String email,
                               @FormParam( "firstName" ) final String firstName,
                               @FormParam( "lastName" ) final String lastName ) {
        peopleService.addPerson( email, firstName, lastName );
        return Response.created( uriInfo.getRequestUriBuilder().path( email ).build() ).build();
    }

*/

	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
	@POST
	public Response addPersonQueryParam(@Context final UriInfo uriInfo,
	                                    @QueryParam("email") final String email,
	                                    @QueryParam("firstName") final String firstName,
	                                    @QueryParam("lastName") final String lastName) {
		peopleService.addPerson(email, firstName, lastName);
		return Response.created(uriInfo.getRequestUriBuilder().path(email).build()).build();
	}

	/* @Produces({MediaType.APPLICATION_JSON})
		@Path("/{email}")
		@PUT
		public Person updatePerson(@PathParam("email") final String email,
								   @FormParam("firstName") final String firstName,
								   @FormParam("lastName") final String lastName) {

			final Person person = peopleService.getByEmail(email);
			if (firstName != null) {
				person.setFirstName(firstName);
			}

			if (lastName != null) {
				person.setLastName(lastName);
			}

			return person;
		}
	*/
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/{email}")
	@HEAD
	public Response checkPerson(@PathParam("email") final String email) {
		if (peopleService.checkPersonByEmail(email)) {
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

	}

	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/{email}")
	@PUT
	public Person updatePersonQueryParam(@PathParam("email") final String email,
	                                     @QueryParam("firstName") final String firstName,
	                                     @QueryParam("lastName") final String lastName) {

		final Person person = peopleService.getByEmail(email);
		if (firstName != null) {
			person.setFirstName(firstName);
		}

		if (lastName != null) {
			person.setLastName(lastName);
		}

		return person;
	}

	@Path("/{email}")
	@DELETE
	public Response deletePerson(@PathParam("email") final String email) {
		peopleService.removePerson(email);
		return Response.ok().build();
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/options")
	@OPTIONS
	public Response getOptions(@Context HttpHeaders headers,
	                           @Context Request request) {
		return Response.ok()
		               .header(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS,
		                       "GET POST DELETE PUT OPTIONS")
		               .header(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS, "false")
		               .header(CorsHeaderConstants.HEADER_AC_REQUEST_HEADERS,
		                       MediaType.APPLICATION_JSON)
		               .build();
	}
}
