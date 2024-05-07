package com.herbmarshall.fault;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class ConstructorResolver<T> {

	static final String NO_PARAMETERS_TO_TRY = "No parameter lists provided, please use tryParameter method";
	static final String CONSTRUCTOR_NOT_FOUND_TEMPLATE = "%s does not have expected constructor";
	static final String INSTANTIATION_FAILURE_TEMPLATE = "Could not create instance of %s";

	private final Class<T> type;
	private final List<Class<?>[]> parameterLists = new ArrayList<>();

	private ConstructorResolver( Class<T> type ) {
		this.type = Objects.requireNonNull( type );
	}

	ConstructorResolver<T> tryParameters( Class<?>... classes ) {
		parameterLists.add( classes );
		return this;
	}

	T create( Object... args ) {
		try {
			return resolve().newInstance( args );
		}
		catch ( InstantiationException | IllegalAccessException | InvocationTargetException e ) {
			throw new UnsupportedOperationException( INSTANTIATION_FAILURE_TEMPLATE.formatted( type ), e );
		}
	}

	private Constructor<T> resolve() {
		Throwable noMethodException = null;
		for ( Class<?>[] parameterList : parameterLists ) {
			try {
				return type.getConstructor( parameterList );
			}
			catch ( NoSuchMethodException e ) {
				noMethodException = e;
			}
		}
		throw noMethodException == null ?
			new IllegalStateException( NO_PARAMETERS_TO_TRY ) :
			new UnsupportedOperationException( CONSTRUCTOR_NOT_FOUND_TEMPLATE.formatted( type ), noMethodException );
	}

	static <T> ConstructorResolver<T> using( Class<T> type ) {
		return new ConstructorResolver<>( type );
	}

}
