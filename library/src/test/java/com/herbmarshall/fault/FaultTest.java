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
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.herbmarshall.fault.Fault.TO_STRING_TEMPLATE;

class FaultTest {

	private static final int MAX_RANDOM_ATTEMPTS = 1000;

	private static final Random random = new Random();
	private static final Map<Class<? extends Throwable>, ThrowableConstructor<?>> exceptions = Map.of(
		Exception.class, new ThrowableConstructor<>(
			Exception::new,
			Exception::new
		),
		Throwable.class, new ThrowableConstructor<>(
			Throwable::new,
			Throwable::new
		),
		IllegalArgumentException.class, new ThrowableConstructor<>(
			IllegalArgumentException::new,
			IllegalArgumentException::new
		),
		RuntimeException.class, new ThrowableConstructor<>(
			RuntimeException::new,
			RuntimeException::new
		),
		UnsupportedOperationException.class, new ThrowableConstructor<>(
			UnsupportedOperationException::new,
			UnsupportedOperationException::new
		),
		IllegalStateException.class, new ThrowableConstructor<>(
			IllegalStateException::new,
			IllegalStateException::new
		),
		AssertionError.class, new ThrowableConstructor<>(
			AssertionError::new,
			AssertionError::new
		)
	);

	@Nested
	class constructor {

		@Test
		void type_null() {
			// Arrange
			String message = randomString();
			// Act
			try {
				new Fault<>( null, message );
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
			// Act
			try {
				new Fault<>( randomType(), null );
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
		Fault<?> fault = new Fault<>( type, randomString() );
		// Act
		Class<?> output = fault.getType();
		// Assert
		Assertions.assertSame( type, output );
	}

	@Test
	void getMessage() {
		// Arrange
		String message = randomString();
		Fault<?> fault = new Fault<>( randomType(), message );
		// Act
		String output = fault.getMessage();
		// Assert
		Assertions.assertSame( message, output );
	}

	@Nested
	class build {

		@Test
		void noArg() {
			exceptions.keySet().forEach( this::noArg );
		}

		@SuppressWarnings( "unchecked" )
		private <E extends Throwable> void noArg( Class<E> type ) {
			Standard.out.println( "Testing type " + type );
			// Arrange
			String message = randomString();
			Fault<E> fault = new Fault<>( type, message );
			// Act
			E output = fault.build();
			// Assert
			E expected = ( E ) exceptions.get( type ).message.apply( message );
			throwableIsEqual( expected, output );
		}

		@Test
		void throwable() {
			exceptions.keySet().forEach( this::throwable );
		}

		@SuppressWarnings( "unchecked" )
		private <E extends Throwable> void throwable( Class<E> type ) {
			Standard.out.println( "Testing type " + type );
			// Arrange
			String message = randomString();
			Fault<E> fault = new Fault<>( type, message );
			Exception cause = new Exception();
			// Act
			E output = fault.build( cause );
			// Assert
			E expected = ( E ) exceptions.get( type ).caused.apply( message, cause );
			throwableIsEqual( expected, output );
		}

		@Test
		void cause_null() {
			exceptions.keySet().forEach( this::cause_null );
		}

		private <E extends Throwable> void cause_null( Class<E> type ) {
			// Arrange
			String message = randomString();
			Fault<E> fault = new Fault<>( type, message );
			// Act
			try {
				fault.build( null );
				Assertions.fail();
			}
			// Assert
			catch ( NullPointerException e ) {
				Assertions.assertEquals(
					Fault.nullPointerError( "cause" ),
					e.getMessage()
				);
			}
		}

	}

	@Nested
	class print_noArg {

		@AfterEach
		void tearDown() {
			Standard.resetAll();
		}

		@Test
		void happyPath() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			Standard.out.override( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> fault = new Fault<>( type, message );
			// Act
			Fault<?> output = fault.print();
			// Assert
			Assertions.assertSame( fault, output );
			Assertions.assertEquals( TO_STRING_TEMPLATE.formatted( type, message ) + "\n", buffer.toString() );
			Standard.out.reset();
		}

	}

	@Nested
	class print_stream {

		@Test
		void happyPath() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream stream = new PrintStream( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> fault = new Fault<>( type, message );
			// Act
			Fault<?> output = fault.print( stream );
			// Assert
			Assertions.assertSame( fault, output );
			Assertions.assertEquals( TO_STRING_TEMPLATE.formatted( type, message ) + "\n", buffer.toString() );
		}

		@Test
		void stream_null() {
			// Arrange
			Fault<?> fault = new Fault<>( randomType(), randomString() );
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
		void happyPath() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			Standard.err.override( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Throwable error = newThrowable( type, message );
			Fault<?> fault = new Fault<>( type, message );
			// Act
			Fault<?> output = fault.validate( error );
			// Assert
			Assertions.assertSame( fault, output );
			Assertions.assertEquals( "", buffer.toString() );
			Standard.err.reset();
		}

		@Test
		void error_null() {
			// Arrange
			Fault<?> fault = new Fault<>( randomType(), randomString() );
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
		void error_badType() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			Standard.err.override( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> fault = new Fault<>( type, message );
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
		void error_wrongMessage() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			Standard.err.override( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> fault = new Fault<>( type, message );
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
		void happyPath() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream stream = new PrintStream( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Throwable error = newThrowable( type, message );
			Fault<?> fault = new Fault<>( type, message );
			// Act
			Fault<?> output = fault.validate( error, stream );
			// Assert
			Assertions.assertSame( fault, output );
			Assertions.assertEquals( "", buffer.toString() );
		}

		@Test
		void error_null() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream stream = new PrintStream( buffer );
			Fault<?> fault = new Fault<>( randomType(), randomString() );
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
			Fault<?> fault = new Fault<>( type, message );
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
		void error_badType() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream stream = new PrintStream( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> fault = new Fault<>( type, message );
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
		void error_wrongMessage() {
			// Arrange
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			PrintStream stream = new PrintStream( buffer );
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> fault = new Fault<>( type, message );
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
		Fault<?> fault = new Fault<>( type, message );
		// Act
		String output = fault.toString();
		// Assert
		Assertions.assertEquals(
			TO_STRING_TEMPLATE.formatted( type, message ),
			output
		);
	}

	@Nested
	class equals {

		@Test
		void happyPath() {
			// Arrange
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> faultA = new Fault<>( type, message );
			Fault<?> faultB = new Fault<>( type, message );
			// Act
			boolean output = faultA.equals( faultB );
			// Assert
			Assertions.assertTrue( output );
		}

		@Test
		@SuppressWarnings( { "ConstantConditions", "UnnecessaryLocalVariable" } )
		void other_same() {
			// Arrange
			Fault<?> faultA = new Fault<>( randomType(), randomString() );
			Fault<?> faultB = faultA;
			// Act
			boolean output = faultA.equals( faultB );
			// Assert
			Assertions.assertTrue( output );
		}

		@Test
		@SuppressWarnings( "ConstantConditions" )
		void other_null() {
			// Arrange
			Fault<?> fault = new Fault<>( randomType(), randomString() );
			// Act
			boolean output = fault.equals( null );
			// Assert
			Assertions.assertFalse( output );
		}

		@Test
		@SuppressWarnings( "EqualsBetweenInconvertibleTypes" )
		void other_wrongType() {
			// Arrange
			Fault<?> faultA = new Fault<>( randomType(), randomString() );
			String faultB = randomString();
			// Act
			boolean output = faultA.equals( faultB );
			// Assert
			Assertions.assertFalse( output );
		}

		@Test
		void type_different() {
			// Arrange
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> faultA = new Fault<>( type, message );
			Fault<?> faultB = new Fault<>(
				randomType( type ),
				message
			);
			// Act
			boolean output = faultA.equals( faultB );
			// Assert
			Assertions.assertFalse( output );
		}

		@Test
		void message_different() {
			// Arrange
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> faultA = new Fault<>( type, message );
			Fault<?> faultB = new Fault<>(
				type,
				randomString( message )
			);
			// Act
			boolean output = faultA.equals( faultB );
			// Assert
			Assertions.assertFalse( output );
		}

	}

	@Nested
	class hashCode {

		@Test
		void happyPath() {
			// Arrange
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> faultA = new Fault<>( type, message );
			Fault<?> faultB = new Fault<>( type, message );
			// Act
			boolean output = faultA.hashCode() == faultB.hashCode();
			// Assert
			Assertions.assertTrue( output );
		}

		@Test
		@SuppressWarnings( "UnnecessaryLocalVariable" )
		void other_same() {
			// Arrange
			Fault<?> faultA = new Fault<>( randomType(), randomString() );
			Fault<?> faultB = faultA;
			// Act
			boolean output = faultA.hashCode() == faultB.hashCode();
			// Assert
			Assertions.assertTrue( output );
		}

		@Test
		void type_different() {
			// Arrange
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> faultA = new Fault<>( type, message );
			Fault<?> faultB = new Fault<>(
				randomType( type ),
				message
			);
			// Act
			boolean output = faultA.hashCode() == faultB.hashCode();
			// Assert
			Assertions.assertFalse( output );
		}

		@Test
		void message_different() {
			// Arrange
			Class<? extends Throwable> type = randomType();
			String message = randomString();
			Fault<?> faultA = new Fault<>( type, message );
			Fault<?> faultB = new Fault<>(
				type,
				randomString( message )
			);
			// Act
			boolean output = faultA.hashCode() == faultB.hashCode();
			// Assert
			Assertions.assertFalse( output );
		}

	}

	private String getStackTrace( Throwable e ) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		e.printStackTrace( new PrintStream( buffer ) );
		return buffer.toString();
	}

	@SuppressWarnings( "unchecked" )
	private <E extends Throwable> E newThrowable( Class<E> type, String message ) {
		return ( E ) exceptions.get( type ).message.apply( message );
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

	private void throwableIsEqual( Throwable expected, Throwable actual ) {
		Assertions.assertEquals( expected.getClass(), actual.getClass() );
		Assertions.assertEquals( expected.getMessage(), actual.getMessage() );
		Assertions.assertEquals( expected.getCause(), actual.getCause() );
	}

	private String randomString() {
		return UUID.randomUUID().toString();
	}

	private String randomString( String exclude ) {
		int count = 0;
		while ( count++ < MAX_RANDOM_ATTEMPTS ) {
			String string = randomString();
			if ( ! string.equals( exclude ) ) return string;
		}
		throw new IllegalStateException( "Could not find String not equal to '" + exclude + "'" );
	}

	private record ThrowableConstructor<E extends Throwable>(
		Function<String, E> message,
		BiFunction<String, Throwable, E> caused
	) {}

}
