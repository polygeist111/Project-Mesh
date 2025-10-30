const name = "James";
const person ={first: name }
console. log(person);
const sayHelloLinting = (fName) => { console. log(`Hello linting, ${fName}`) }
sayHelloLinting('James');
// this code is not project-relevant and will be removed
// it is simply to test that Prettier is functioning correctly as a pre-commit action
// adding change to stage :3