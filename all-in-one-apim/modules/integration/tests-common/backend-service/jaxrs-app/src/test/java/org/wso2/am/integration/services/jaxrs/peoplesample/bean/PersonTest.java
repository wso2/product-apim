package org.wso2.am.integration.services.jaxrs.peoplesample.bean;

import org.junit.Assert;
import org.junit.Test;

public class PersonTest {

    @Test
    public void testPersonConstructorAndGetters() {

        Person person = new Person();

        person.setEmail("john@example.com");
        person.setFirstName("John");
        person.setLastName("Doe");

        Assert.assertEquals("john@example.com", person.getEmail());
        Assert.assertEquals("John", person.getFirstName());
        Assert.assertEquals("Doe", person.getLastName());
    }

    @Test
    public void testPersonEquality() {

        Person p1 = new Person();
        p1.setEmail("test@example.com");

        Person p2 = new Person();
        p2.setEmail("test@example.com");

        Assert.assertEquals(p1.getEmail(), p2.getEmail());
    }
}
