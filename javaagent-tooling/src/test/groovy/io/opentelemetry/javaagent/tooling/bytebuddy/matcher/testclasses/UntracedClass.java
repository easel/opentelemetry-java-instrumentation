/*
 * Copyright The OpenTelemetry Authors
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

package io.opentelemetry.javaagent.tooling.bytebuddy.matcher.testclasses;

public class UntracedClass extends G {
  @Override
  public void g() {}

  @Override
  public void f() {}

  @Override
  public void e() {}

  @Override
  public void d() {}

  @Override
  public void c() {}

  @Override
  public void b() {}

  @Override
  public void a() {}
}