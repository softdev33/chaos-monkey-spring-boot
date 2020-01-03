/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.codecentric.spring.boot.chaos.monkey.watcher;

import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

/** @author Benjamin Wilms */
abstract class ChaosMonkeyBaseAspect {

  @Pointcut("within(de.codecentric.spring.boot.chaos.monkey..*)")
  public void classInChaosMonkeyPackage() {}

  @Pointcut("execution(* *.*(..))")
  public void allPublicMethodPointcut() {}

  String calculatePointcut(String target) {
    return target.replaceAll("\\(\\)", "").replaceAll("\\)", "").replaceAll("\\(", ".");
  }

  String createSignature(MethodSignature signature) {
    return signature.getDeclaringTypeName() + "." + signature.getMethod().getName();
  }
}
