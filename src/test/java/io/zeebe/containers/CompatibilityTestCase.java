/*
 * Copyright © 2019 camunda services GmbH (info@camunda.com)
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
package io.zeebe.containers;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public abstract class CompatibilityTestCase {
  @Parameter(0)
  public String version;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Stream.of("0.20.0", "0.21.0-alpha1", "0.21.0-alpha2")
        .map(version -> new Object[] {version})
        .collect(Collectors.toList());
  }
}
