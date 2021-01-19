/*
 * Copyright 2021 Trustin Heuiseung Lee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.motd.maven.os;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface exposing file operations.
 */
public interface FileActionFacade {

  /**
   * Gets a {@link InputStream} for reading the content of the file with the specified name.
   *
   * @param      fileName   the system-dependent file name.
   * @return     the {@link InputStream} that can be read to get the file content.
   * @throws     IOException if the file does not exist, is a directory rather than a regular
   *             file, or for some other reason cannot be opened for reading.
   */
  InputStream readFile(String fileName) throws IOException;
}
