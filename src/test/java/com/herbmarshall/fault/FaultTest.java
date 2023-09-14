package com.herbmarshall.fault;

import com.herbmarshall.standardPipe.Standard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.herbmarshall.fault.Fault.*;

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
	class build_noArg {

		@Test
		void happyPath() {
			exceptions.keySet().forEach( this::happyPath );
		}

		@SuppressWarnings( "unchecked" )
		private <E extends Throwable> void happyPath( Class<E> type ) {
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
		void error_noMatchingConstructor() {
			// Arrange
			String message = randomString();
			Fault<NoConstructor> fault = new Fault<>( NoConstructor.class, message );
			// Act
			try {
				fault.build();
				Assertions.fail();
			}
			// Assert
			catch ( UnsupportedOperationException e ) {
				Assertions.assertEquals(
					CONSTRUCTOR_LOCATION_FAILURE_TEMPLATE.formatted( NoConstructor.class ),
					e.getMessage()
				);
				Assertions.assertEquals(
					NoSuchMethodException.class,
					e.getCause().getClass()
				);
			}
		}

		@Test
		void error_inaccessibleConstructor() {
			// Arrange
			String message = randomString();
			Fault<InaccessibleConstructor> fault = new Fault<>( InaccessibleConstructor.class, message );
			// Act
			try {
				fault.build();
				Assertions.fail();
			}
			// Assert
			catch ( UnsupportedOperationException e ) {
				Assertions.assertEquals(
					CONSTRUCTOR_LOCATION_FAILURE_TEMPLATE.formatted( InaccessibleConstructor.class ),
					e.getMessage()
				);
				Assertions.assertEquals(
					NoSuchMethodException.class,
					e.getCause().getClass()
				);
			}
		}

		@Test
		void error_constructorException() {
			// Arrange
			String message = randomString();
			Fault<FailingConstructor> fault = new Fault<>( FailingConstructor.class, message );
			// Act
			try {
				fault.build();
				Assertions.fail();
			}
			// Assert
			catch ( UnsupportedOperationException e ) {
				Assertions.assertEquals(
					INSTANTIATION_FAILURE_TEMPLATE.formatted( FailingConstructor.class ),
					e.getMessage()
				);
				Assertions.assertEquals(
					InvocationTargetException.class,
					e.getCause().getClass()
				);
			}
		}

		@Test
		void error_abstractClass() {
			// Arrange
			String message = randomString();
			Fault<AbstractException> fault = new Fault<>( AbstractException.class, message );
			// Act
			try {
				fault.build();
				Assertions.fail();
			}
			// Assert
			catch ( UnsupportedOperationException e ) {
				Assertions.assertEquals(
					INSTANTIATION_FAILURE_TEMPLATE.formatted( AbstractException.class ),
					e.getMessage()
				);
				Assertions.assertEquals(
					InstantiationException.class,
					e.getCause().getClass()
				);
			}
		}

	}

	@Nested
	class build_Throwable {

		@Test
		void happyPath() {
			exceptions.keySet().forEach( this::happyPath );
		}

		@SuppressWarnings( "unchecked" )
		private <E extends Throwable> void happyPath( Class<E> type ) {
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

		@Test
		void error_noMatchingConstructor() {
			// Arrange
			String message = randomString();
			Fault<NoConstructor> fault = new Fault<>( NoConstructor.class, message );
			Exception cause = new Exception();
			// Act
			try {
				fault.build( cause );
				Assertions.fail();
			}
			// Assert
			catch ( UnsupportedOperationException e ) {
				Assertions.assertEquals(
					CONSTRUCTOR_LOCATION_FAILURE_TEMPLATE.formatted( NoConstructor.class ),
					e.getMessage()
				);
				Assertions.assertEquals(
					NoSuchMethodException.class,
					e.getCause().getClass()
				);
			}
		}

		@Test
		void error_inaccessibleConstructor() {
			// Arrange
			String message = randomString();
			Fault<InaccessibleConstructor> fault = new Fault<>( InaccessibleConstructor.class, message );
			Exception cause = new Exception();
			// Act
			try {
				fault.build( cause );
				Assertions.fail();
			}
			// Assert
			catch ( UnsupportedOperationException e ) {
				Assertions.assertEquals(
					CONSTRUCTOR_LOCATION_FAILURE_TEMPLATE.formatted( InaccessibleConstructor.class ),
					e.getMessage()
				);
				Assertions.assertEquals(
					NoSuchMethodException.class,
					e.getCause().getClass()
				);
			}
		}

		@Test
		void error_constructorException() {
			// Arrange
			String message = randomString();
			Fault<FailingConstructor> fault = new Fault<>( FailingConstructor.class, message );
			Exception cause = new Exception();
			// Act
			try {
				fault.build( cause );
				Assertions.fail();
			}
			// Assert
			catch ( UnsupportedOperationException e ) {
				Assertions.assertEquals(
					INSTANTIATION_FAILURE_TEMPLATE.formatted( FailingConstructor.class ),
					e.getMessage()
				);
				Assertions.assertEquals(
					InvocationTargetException.class,
					e.getCause().getClass()
				);
			}
		}

		@Test
		void error_abstractClass() {
			// Arrange
			String message = randomString();
			Fault<AbstractException> fault = new Fault<>( AbstractException.class, message );
			Exception cause = new Exception();
			// Act
			try {
				fault.build( cause );
				Assertions.fail();
			}
			// Assert
			catch ( UnsupportedOperationException e ) {
				Assertions.assertEquals(
					INSTANTIATION_FAILURE_TEMPLATE.formatted( AbstractException.class ),
					e.getMessage()
				);
				Assertions.assertEquals(
					InstantiationException.class,
					e.getCause().getClass()
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

	private record ThrowableConstructor<E extends Throwable>(
		Function<String, E> message,
		BiFunction<String, Throwable, E> caused
	) {}

	public static class NoConstructor extends Exception {}

	@SuppressWarnings( "unused" )
	public static final class InaccessibleConstructor extends Exception {
		private InaccessibleConstructor( String message ) {
			super( message );
		}
		private InaccessibleConstructor( String message, Throwable cause ) {
			super( message, cause );
		}
	}

	@SuppressWarnings( { "unused", "checkstyle:RedundantModifier" } )
	public abstract static class AbstractException extends Exception {
		public AbstractException( String message ) {
			super( message );
		}
		public AbstractException( String message, Throwable cause ) {
			super( message, cause );
		}
	}

	@SuppressWarnings( { "unused", "checkstyle:RedundantModifier" } )
	public static class FailingConstructor extends Exception {
		public FailingConstructor( String message ) {
			throw new RuntimeException( "Expected Failure" );
		}
		public FailingConstructor( String message, Throwable cause ) {
			throw new RuntimeException( "Expected Failure" );
		}
	}

}
