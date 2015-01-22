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

import org.springframework.stereotype.Service;
import org.wso2.am.integration.services.jaxrs.peoplesample.bean.Person;
import org.wso2.am.integration.services.jaxrs.peoplesample.exceptions.PersonAlreadyExistsException;
import org.wso2.am.integration.services.jaxrs.peoplesample.exceptions.PersonNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PeopleService {
	private ConcurrentMap<String, Person> persons = new ConcurrentHashMap<String, Person>();

	public void setInitPeople() {

		for (int count = 1; count <= 10; count++) {
			Person newPerson = new Person();
			newPerson.setEmail(String.format("test-%d@wso2.com", count));
			newPerson.setFirstName(String.format("testUser%d", count));
			newPerson.setLastName(String.format("testLasrName%d", count));
			persons.putIfAbsent(String.format("test-%d@wso2.com", count), newPerson);
		}
	}

	public Collection<Person> getPeople(int page, int pageSize) {
		Collection<Person> person = new ArrayList<Person>(pageSize);

		for (int index = 0; index < pageSize; ++index) {
			person.add(new Person(
					String.format("person+%d@at.com", (pageSize * (page - 1) + index + 1))));
		}
		setInitPeople();
		return person;
	}

	public Person addPerson(String email) {

		return new Person(email);
	}

	public Person getByEmail(final String email) {
		final Person person = persons.get(email);
		if (person == null) {
			throw new PersonNotFoundException(email);
		}

		return person;
	}

	public boolean checkPersonByEmail(final String email) {
		boolean personExists = true;
		final Person person = persons.get(email);
		if (person == null) {
			personExists = false;
		}
		return personExists;
	}

	public Person addPerson(final String email, final String firstName, final String lastName) {
		final Person person = new Person(email);
		person.setFirstName(firstName);
		person.setLastName(lastName);

		if (persons.putIfAbsent(email, person) != null) {
			throw new PersonAlreadyExistsException(email);
		}

		return person;
	}

	public void removePerson(final String email) {
		if (persons.remove(email) == null) {
			throw new PersonNotFoundException(email);
		}
	}

}
