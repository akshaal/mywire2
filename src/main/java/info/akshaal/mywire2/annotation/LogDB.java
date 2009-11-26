/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package info.akshaal.mywire2.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;

import com.google.inject.BindingAnnotation;

@Retention (RetentionPolicy.RUNTIME)
@Target ({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Inherited
@BindingAnnotation
public @interface LogDB {
}
