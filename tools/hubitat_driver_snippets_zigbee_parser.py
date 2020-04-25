#  Copyright 2019 Markus Liljergren
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

"""
  Snippets used by hubitat-driver-helper-tool
"""
def getGenericZigbeeParseHeader(loglevel=1):
    return """// parse() Generic Zigbee-device header BEGINS here
logging("PARSE START---------------------", 1)
logging("Parsing: ${description}", 0)
ArrayList<String> cmd = []
def msgMap = zigbee.parseDescriptionAsMap(description)
logging("msgMap: ${msgMap}", """ + str(loglevel) + """)
// parse() Generic header ENDS here"""

def getGenericZigbeeParseFooter():
    return """// parse() Generic Zigbee-device footer BEGINS here
logging("PARSE END-----------------------", 1)
return cmd
// parse() Generic footer ENDS here"""
