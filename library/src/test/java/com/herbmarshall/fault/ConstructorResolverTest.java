package com.herbmarshall.fault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static com.herbmarshall.fault.ConstructorResolver.*;

@SuppressWarnings( "unused" )
class ConstructorResolverTest {

	private static final Random random = new Random();

	@Nested
	class constructor {

		@Test
		void type_null() {
			// Arrange
			// Act
			try {
				ConstructorResolver.using( null );
				Assertions.fail();
			}
			// Assert
			catch ( NullPointerException ignored ) {
			}
		}

	}

	@Nested
	class create {

		@Test
		void happyPath() {
			// Arrange
			UUID argument = random();
			ConstructorResolver<StandardConstructor> resolver = ConstructorResolver.using( StandardConstructor.class )
				.tryParameters( UUID.class );
			// Act
			StandardConstructor output = resolver.create( argument );
			// Assert
			Assertions.assertEquals( argument, output.value );
		}

		@Test
		void genericallyTypedArgument() {
			// Arrange
			Object argument = random();  // Variable is typed as Object
			ConstructorResolver<StandardConstructor> resolver = ConstructorResolver.using( StandardConstructor.class )
				.tryParameters( UUID.class );
			// Act
			StandardConstructor output = resolver.create( argument );
			// Assert
			Assertions.assertEquals( argument, output.value );
		}

		@Test
		void genericallyTypedParameter() {
			error(
				ObjectConstructor.class,
				CONSTRUCTOR_NOT_FOUND_TEMPLATE.formatted( ObjectConstructor.class )
			);
		}

		@Test
		void genericallyTypedParameterDoubleCheck() {
			// Arrange
			UUID argument = random();
			ConstructorResolver<ObjectConstructor> resolver = ConstructorResolver.using( ObjectConstructor.class )
				.tryParameters( UUID.class )
				.tryParameters( Object.class );
			// Act
			ObjectConstructor output = resolver.create( argument );
			// Assert
			Assertions.assertEquals( argument, output.value );
		}

		@Test
		void dualTypedParameter() {
			// Arrange
			UUID argument = random();
			ConstructorResolver<DualConstructor> resolver = ConstructorResolver.using( DualConstructor.class )
				.tryParameters( UUID.class )
				.tryParameters( Object.class );
			// Act
			DualConstructor output = resolver.create( argument );
			// Assert
			Assertions.assertEquals( argument, output.valueFromUUID );
		}

		@Test
		void dualTypedParameterReversed() {
			// Arrange
			UUID argument = random();
			ConstructorResolver<DualConstructor> resolver = ConstructorResolver.using( DualConstructor.class )
				.tryParameters( Object.class )
				.tryParameters( UUID.class );
			// Act
			DualConstructor output = resolver.create( argument );
			// Assert
			Assertions.assertEquals( argument, output.valueFromObject );
		}

		@Test
		void noArgConstructor() {
			error(
				NoArgConstructor.class,
				CONSTRUCTOR_NOT_FOUND_TEMPLATE.formatted( NoArgConstructor.class )
			);
		}

		@Test
		void inaccessibleConstructor() {
			error(
				InaccessibleConstructor.class,
				CONSTRUCTOR_NOT_FOUND_TEMPLATE.formatted( InaccessibleConstructor.class )
			);
		}

		@Test
		void abstractClass() {
			error(
				AbstractClass.class,
				INSTANTIATION_FAILURE_TEMPLATE.formatted( AbstractClass.class )
			);
		}

		@Test
		void failingConstructor() {
			error(
				FailingConstructor.class,
				INSTANTIATION_FAILURE_TEMPLATE.formatted( FailingConstructor.class )
			);
		}

		private <T> void error( Class<T> type, String errorMessage ) {
			// Arrange
			UUID argument = random();
			ConstructorResolver<T> resolver = ConstructorResolver.using( type )
				.tryParameters( UUID.class );
			// Act
			try {
				resolver.create( argument );
				Assertions.fail();
			}
			// Assert
			catch ( UnsupportedOperationException e ) {
				Assertions.assertEquals( errorMessage, e.getMessage() );
			}
		}

		@Test
		void doubleCheckFailingConstructor() {
			// Arrange
			UUID argument = random();
			ConstructorResolver<FailingDualConstructor> resolver =
				ConstructorResolver.using( FailingDualConstructor.class )
				.tryParameters( UUID.class )
				.tryParameters( Object.class );
			// Act
			try {
				resolver.create( argument );
				Assertions.fail();
			}
			// Assert
			catch ( UnsupportedOperationException e ) {
				Assertions.assertEquals(
					INSTANTIATION_FAILURE_TEMPLATE.formatted( FailingDualConstructor.class ),
					e.getMessage()
				);
			}
		}

		@Test
		void noParametersProvided() {
			// Arrange
			UUID argument = random();
			ConstructorResolver<StandardConstructor> resolver = ConstructorResolver.using( StandardConstructor.class );
			// Act
			try {
				resolver.create( argument );
				Assertions.fail();
			}
			// Assert
			catch ( IllegalStateException e ) {
				Assertions.assertEquals(
					NO_PARAMETERS_TO_TRY,
					e.getMessage()
				);
			}
		}

	}

	private UUID random() {
		return UUID.randomUUID();
	}

	@SuppressWarnings( "checkstyle:RedundantModifier" )
	static class StandardConstructor {
		final UUID value;
		public StandardConstructor( UUID value ) {
			this.value = value;
		}
	}

	@SuppressWarnings( "checkstyle:RedundantModifier" )
	static class ObjectConstructor {
		final Object value;
		public ObjectConstructor( Object value ) {
			this.value = value;
		}
	}

	@SuppressWarnings( "checkstyle:RedundantModifier" )
	static class DualConstructor {
		final UUID valueFromUUID;
		final Object valueFromObject;
		public DualConstructor( UUID value ) {
			this.valueFromUUID = value;
			this.valueFromObject = null;
		}
		public DualConstructor( Object value ) {
			this.valueFromUUID = null;
			this.valueFromObject = value;
		}
	}

	@SuppressWarnings( "checkstyle:RedundantModifier" )
	static class FailingDualConstructor {
		final Object value;
		public FailingDualConstructor( UUID value ) {
			throw new RuntimeException( "Expected Failure" );
		}
		public FailingDualConstructor( Object value ) {
			this.value = value;
		}
	}

	static class NoArgConstructor {}

	static final class InaccessibleConstructor {
		private InaccessibleConstructor( UUID value ) {}
	}

	@SuppressWarnings( "checkstyle:RedundantModifier" )
	abstract static class AbstractClass {
		public AbstractClass( UUID value ) {}
	}

	@SuppressWarnings( "checkstyle:RedundantModifier" )
	static class FailingConstructor {
		public FailingConstructor( UUID value ) {
			throw new RuntimeException( "Expected Failure" );
		}
	}

}
