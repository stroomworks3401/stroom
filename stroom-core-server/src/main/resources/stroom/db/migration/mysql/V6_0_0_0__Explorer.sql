/*
 * Copyright 2016 Crown Copyright
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

CREATE TABLE EXPLORER_TREE_NODE (
  ID 				int(11) NOT NULL AUTO_INCREMENT,
  TYPE 				varchar(255) NOT NULL,
  UUID 				varchar(255) NOT NULL,
  NAME				varchar(255) NOT NULL,
  PRIMARY KEY       (ID),
  UNIQUE 			(TYPE, UUID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE EXPLORER_TREE_PATH (
  ANCESTOR 			int(11) NOT NULL,
  DESCENDANT 		int(11) NOT NULL,
  DEPTH 			int(11) NOT NULL,
  ORDER_INDEX		int(11) NOT NULL,
  PRIMARY KEY       (ANCESTOR, DESCENDANT)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;