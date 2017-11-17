package server.service;

import server.obj.Employee;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/employeeservice/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EmployeeService {

	Map<String, Employee> employeeMap = new HashMap<String, Employee>();


	public void init() {

		Employee employeeOne = new Employee();
		employeeOne.setId(1);
		employeeOne.setName("Alvin Reyes");

		Employee employeeTwo = new Employee();
		employeeTwo.setId(2);
		employeeTwo.setName("Rachelle Ann");

		employeeMap.put("1", employeeOne);
		employeeMap.put("2", employeeTwo);

	}

	public EmployeeService() {
		init();
	}

	@GET
	@Path("/employee/{id}/")
	public Employee getEmployee(@PathParam("id") String id, @Context HttpHeaders headers) { ;
		Employee c = employeeMap.get(id);
		return c;
	}
}