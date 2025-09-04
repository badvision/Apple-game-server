/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ags.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate configuration variables
 * @author brobert
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Configurable {
    public enum CATEGORY { COM, RUNTIME, ADVANCED };
    CATEGORY category();
    boolean isRequired();
}
