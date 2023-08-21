package com.herbmarshall.fault;

import com.herbmarshall.standardPipe.Standard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;

import static com.herbmarshall.fault.Fault.TO_STRING_TEMPLATE;

class FaultTest {

	private static final int MAX_RANDOM_ATTEMPTS = 1000;

	private static final Random random = new Random();
	private static final Map<Class<? extends Throwable>, Function<String, Throwable>> exceptions = Map.of(
			Exception.class, Exception::new,
			Throwable.class, Throwable::new,
			IllegalArgumentException.class, IllegalArgumentException::new,
			RuntimeException.class, RuntimeException::new,
			UnsupportedOperationException.class, UnsupportedOperationException::new,
			IllegalStateException.class, IllegalStateException::new
		);

	@Nested
	class constructor {

		@Test
		void type_null() {
			// Arrange
			String message = randomString();
			// Act
			try {
				new Fault( null, message );
				Assertions.fail();
			}
			// Assert
			catch ( NullPointerException e ) {
				Assertions.assertEquals(
					Fault.nullPointerError( "type" ),
					e.getMessage()
				);
			}
		}

		@Test
		void message_null() {
			// Arrange
			Class<? extends Throwable> type = randomType();
			// Act
			try {
				new Fault( type, null );
				Assertions.fail();
			}
			// Assert
			catch ( NullPointerException e ) {
				Assertions.assertEquals(
					Fault.nullPointerError( "message" ),
					e.getMessage()
				);
			}
		}

	}

	@Test
	void getErrorType() {
		// Arrange
		Class<? extends Throwable> type = randomType();
		Fault fault = new Fault( type, randomString() );
		// Act
		Class<? extends Throwable> output = fault.getType();
		// Assert
		Assertions.assertSame( type, output );
	}

	@Test
	void getMessage() {
		// Arrange
		String message = randomString();
		Fault fault = new Fault( randomType(), message );
		// Act
		String output = fault.getMessage();
		// Assert
		Assertions.assertSame( message, output );
	}

	@Nested
	class print_noArg {

		@AfterEach
		void tearDown() {
			Standard.resetAll();
		}

		@Test
		void $happyPath() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			Standard.out.override( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault fault = new Fault( type, message );
			// Act
			Fault output = fault.print();
			// Assert
			Assertions.assertSame( fault, output );
			Assertions.assertEquals( TO_STRING_TEMPLATE.formatted( type, message ) + "\n", buffer.toString() );
			Standard.out.reset();
		}

	}

	@Nested
	class print_stream {

		@Test
		void $happyPath() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream stream = new PrintStream( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault fault = new Fault( type, message );
			// Act
			Fault output = fault.print( stream );
			// Assert
			Assertions.assertSame( fault, output );
			Assertions.assertEquals( TO_STRING_TEMPLATE.formatted( type, message ) + "\n", buffer.toString() );
		}

		@Test
		void stream_null() {
			// Arrange
			Fault fault = new Fault( randomType(), randomString() );
			// Act
			try {
				fault.print( null );
				Assertions.fail();
			}
			// Assert
			catch ( NullPointerException e ) {
				Assertions.assertEquals(
					Fault.nullPointerError( "stream" ),
					e.getMessage()
				);
			}
		}

	}


	@Nested
	class validate_Throwable {

		@AfterEach
		void tearDown() {
			Standard.resetAll();
		}

		@Test
		void $happyPath() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			Standard.err.override( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Throwable error = newThrowable( type, message );
			Fault fault = new Fault( type, message );
			// Act
			Fault output = fault.validate( error );
			// Assert
			Assertions.assertSame( fault, output );
			Assertions.assertEquals( "", buffer.toString() );
			Standard.err.reset();
		}

		@Test
		void error_null() {
			// Arrange
			Fault fault = new Fault( randomType(), randomString() );
			// Act
			try {
				fault.validate( null );
				Assertions.fail();
			}
			// Assert
			catch ( NullPointerException e ) {
				Assertions.assertEquals(
					Fault.nullPointerError( "error" ),
					e.getMessage()
				);
			}
		}

		@Test
		void $error_badType() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			Standard.err.override( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault fault = new Fault( type, message );
			Class<? extends Throwable> badType = randomType( type );
			Throwable badError = newThrowable( badType, message );
			// Act
			try {
				fault.validate( badError );
				Assertions.fail();
			}
			// Assert
			catch ( AssertionError e ) {
				Assertions.assertEquals(
					Fault.typeError( type, badType ),
					e.getMessage()
				);
				Assertions.assertEquals(
					getStackTrace( badError ),
					buffer.toString()
				);
			}
			finally {
				Standard.err.reset();
			}
		}

		@Test
		void $error_wrongMessage() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			Standard.err.override( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault fault = new Fault( type, message );
			String badMessage = randomString();
			Throwable error = newThrowable( type, badMessage );
			// Act
			try {
				fault.validate( error );
				Assertions.fail();
			}
			// Assert
			catch ( AssertionError e ) {
				Assertions.assertEquals(
					Fault.messageError( message, badMessage ),
					e.getMessage()
				);
				Assertions.assertEquals(
					getStackTrace( error ),
					buffer.toString()
				);
			}
			finally {
				Standard.err.reset();
			}
		}

	}

	@Nested
	class validate_Throwable_PrintStream {

		@Test
		void $happyPath() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream stream = new PrintStream( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Throwable error = newThrowable( type, message );
			Fault fault = new Fault( type, message );
			// Act
			Fault output = fault.validate( error, stream );
			// Assert
			Assertions.assertSame( fault, output );
			Assertions.assertEquals( "", buffer.toString() );
		}

		@Test
		void error_null() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream stream = new PrintStream( buffer );
			Fault fault = new Fault( randomType(), randomString() );
			// Act
			try {
				fault.validate( null, stream );
				Assertions.fail();
			}
			// Assert
			catch ( NullPointerException e ) {
				Assertions.assertEquals(
					Fault.nullPointerError( "error" ),
					e.getMessage()
				);
			}
		}

		@Test
		void stream_null() {
			// Arrange
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault fault = new Fault( type, message );
			Throwable error = newThrowable( type, message );
			// Act
			try {
				fault.validate( error, null );
				Assertions.fail();
			}
			// Assert
			catch ( NullPointerException e ) {
				Assertions.assertEquals(
					Fault.nullPointerError( "stream" ),
					e.getMessage()
				);
			}
		}

		@Test
		void $error_badType() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream stream = new PrintStream( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault fault = new Fault( type, message );
			Class<? extends Throwable> badType = randomType( type );
			Throwable badError = newThrowable( badType, message );
			// Act
			try {
				fault.validate( badError, stream );
				Assertions.fail();
			}
			// Assert
			catch ( AssertionError e ) {
				Assertions.assertEquals(
					Fault.typeError( type, badType ),
					e.getMessage()
				);
				Assertions.assertEquals(
					getStackTrace( badError ),
					buffer.toString()
				);
			}
		}

		@Test
		void $error_wrongMessage() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream stream = new PrintStream( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault fault = new Fault( type, message );
			String badMessage = randomString();
			Throwable error = newThrowable( type, badMessage );
			// Act
			try {
				fault.validate( error, stream );
				Assertions.fail();
			}
			// Assert
			catch ( AssertionError e ) {
				Assertions.assertEquals(
					Fault.messageError( message, badMessage ),
					e.getMessage()
				);
				Assertions.assertEquals(
					getStackTrace( error ),
					buffer.toString()
				);
			}
		}

	}

	@Test
	void toString_() {
		// Arrange
		Class<? extends Throwable> type = randomType();
		String message = randomString();
		Fault fault = new Fault( type, message );
		// Act
		String output = fault.toString();
		// Assert
		Assertions.assertEquals(
			TO_STRING_TEMPLATE.formatted( type, message ),
			output
		);
	}

	private String getStackTrace( Throwable e ) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		e.printStackTrace( new PrintStream( buffer ) );
		return buffer.toString();
	}

	@SuppressWarnings( "unchecked" )
	private <E extends Throwable> E newThrowable( Class<E> type, String message ) {
		return ( E ) exceptions.get( type ).apply( message );
	}

	private Class<? extends Throwable> randomType() {
		return exceptions.keySet()
			.stream()
			.skip( random.nextInt( exceptions.size() ) )
			.findFirst()
			.orElseThrow();
	}

	private Class<? extends Throwable> randomType( Class<? extends Throwable> exclude ) {
		int count = 0;
		while ( count++ < MAX_RANDOM_ATTEMPTS ) {
			Class<? extends Throwable> exception = randomType();
			if ( exception != exclude ) return exception;
		}
		throw new IllegalStateException( "Could not find Throwable not equal to " + exclude.getSimpleName() );
	}

	private String randomString() {
		return UUID.randomUUID().toString();
	}

}
