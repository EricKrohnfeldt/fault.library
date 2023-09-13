package com.herbmarshall.fault;

import com.herbmarshall.standardPipe.Standard;

import java.io.PrintStream;
import java.util.Objects;

/**
 * Utility to build error message containers.
 * @param <E> The error type
 */
public final class Fault<E extends Throwable> {

	static final String TO_STRING_TEMPLATE = "(%s) %s";

	private final Class<E> type;
	private final String message;

	/** Create instance. */
	public Fault( Class<E> type, String message ) {
		this.type = requireNonNull( type, "type" );
		this.message = requireNonNull( message, "message" );
	}

	/** @return the generated error type. */
	public Class<E> getType() {
		return type;
	}

	/** @return the generated error message. */
	public String getMessage() {
		return message;
	}

	/**
	 * Will print the error type and message to {@link Standard#out}.
	 * @return Self reference
	 */
	public Fault<E> print() {
		return print( Standard.out.toStream() );
	}

	/**
	 * Will print the error type and message to {@code stream}.
	 * @return Self reference
	 * @throws NullPointerException if {@code stream} is null.
	 */
	public Fault<E> print( PrintStream stream ) {
		requireNonNull( stream, "stream" ).println( this );
		return this;
	}

	/**
	 * Will compare {@code error} class type and message.
	 * On failure, it will print the error stack trace to {@link Standard#err}.
	 * @return Self reference
	 * @throws AssertionError if the type or message do not match
	 * @see Standard#err
	 */
	public Fault<E> validate( Throwable error ) {
		return validate( error, Standard.err.toStream() );
	}

	/**
	 * Will compare {@code error} class type and message.
	 * On failure, it will print the error stack trace to {@code stream}.
	 * @return Self reference
	 * @throws AssertionError if either type or message do not match
	 * @throws NullPointerException if either {@code error} or {@code stream} are null.
	 */
	public Fault<E> validate( Throwable error, PrintStream stream ) {
		requireNonNull( error, "error" );
		requireNonNull( stream, "stream" );
		try {
			validateType( error );
			validateMessage( error );
		}
		catch ( AssertionError e ) {
			error.printStackTrace( stream );
			throw e;
		}
		return this;
	}

	private void validateType( Throwable throwable ) {
		if ( throwable.getClass().equals( type ) ) return;
		throw new AssertionError( typeError( type, throwable.getClass() ) );
	}

	private void validateMessage( Throwable throwable ) {
		if ( throwable.getMessage().equals( message ) ) return;
		throw new AssertionError( messageError( message, throwable.getMessage() ) );
	}

	@Override
	public String toString() {
		return TO_STRING_TEMPLATE.formatted( type, message );
	}

	private <T> T requireNonNull( T value, String name ) {
		return Objects.requireNonNull( value, nullPointerError( name ) );
	}

	static String typeError( Class<? extends Throwable> expected, Class<? extends Throwable> actual ) {
		return "Incorrect error type; expected " + expected + " but received " + actual;
	}

	static String messageError( String expected, String actual ) {
		return "Incorrect error message; expected " + expected + " but received " + actual;
	}

	static String nullPointerError( String parameterName ) {
		return "Value of " + parameterName + " cannot be null";
	}

}
