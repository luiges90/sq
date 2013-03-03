/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sq.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * @author Peter
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Min {
    double value();
}
