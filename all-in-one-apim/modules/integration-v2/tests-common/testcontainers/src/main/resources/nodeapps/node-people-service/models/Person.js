class Person {
    constructor(email, firstName = '', lastName = '') {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}

module.exports = Person;
