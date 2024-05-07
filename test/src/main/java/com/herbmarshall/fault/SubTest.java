package com.herbmarshall.fault;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for method the perform a test, but not directly called by JUnit.
 */
@Target( ElementType.METHOD )
@Retention( RetentionPolicy.SOURCE )
public @interface SubTest {

}
