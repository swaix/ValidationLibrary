# Validation Library

Library used to validate form fields programmatically.
This library is entirely written in kotlin but you can use it also in Java.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Installing

**Step 1**. Add the JitPack repository to your build file.

Add it in your root build.gradle at the end of repositories:
```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

**Step 2**. Add the dependency

```	
	dependencies {
	        compile 'com.github.swaix:ValidationLibrary:1.2.6'
	}
```

**Step 3**. Create a ValidItem class


```	
data class RegisterItem(
	@Bind(R.id.register_field_id) 
	@Required 
	@EmailType 
	@ErrorMessage(R.string.error_message_field) 
	var field: String? = null) : ValidItem()
```	

**@Bind** : use this annotation to bind your field to a view through its id. You can bind also through a Map<Field,View>

**@Required**: use this annotation if your field is mandatory (it doens't accept null or empty value for it)

**@EmailType**: use this annotation in order to check email pattern.

**@PasswordType**: use this annotation in order to check password pattern. (You can override pattern)

**@NumberBetween**: use this annotation to validate a number between min and max value.

**@ErrorMessage**: use this annotation to add a custom error, default error is "Error". This error will be shown if your view is an EditText


## Authors

* **Alessandro Finocchiaro**  - [SwaiX](https://github.com/swaix)


