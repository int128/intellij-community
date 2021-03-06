/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.externalSystem.model.task.event;

/**
 * @author Vladislav.Soroka
 * @since 12/2/2015
 */
public class DefaultOperationResult implements OperationResult {

  private final long myStartTime;
  private final long myEndTime;

  public DefaultOperationResult(long startTime, long endTime) {
    myStartTime = startTime;
    myEndTime = endTime;
  }

  @Override
  public long getStartTime() {
    return myStartTime;
  }

  @Override
  public long getEndTime() {
    return myEndTime;
  }
}
