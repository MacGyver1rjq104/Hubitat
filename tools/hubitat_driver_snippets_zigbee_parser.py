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
def getGenericZigbeeParseHeader(loglevel=0):
    return """// parse() Generic Zigbee-device header BEGINS here
logging("PARSE START---------------------", """ + str(loglevel) + """)
logging("Parsing: '${description}'", """ + str(loglevel) + """)
ArrayList<String> cmd = []
Map msgMap = null
if(description.indexOf('encoding: 4C') >= 0) {
  // Parsing of STRUCT (4C) is broken in HE, for now we need a workaround
  msgMap = unpackStructInMap(zigbee.parseDescriptionAsMap(description.replace('encoding: 4C', 'encoding: F2')))
} else if(description.indexOf('attrId: FF01, encoding: 42') >= 0) {
  msgMap = zigbee.parseDescriptionAsMap(description.replace('encoding: 42', 'encoding: F2'))
  msgMap["encoding"] = "41"
  msgMap["value"] = parseXiaomiStruct(msgMap["value"], isFCC0=false, hasLength=true)
} else {
  msgMap = zigbee.parseDescriptionAsMap(description)
}
logging("msgMap: ${msgMap}", """ + str(loglevel) + """)
// parse() Generic header ENDS here"""

def getGenericZigbeeParseFooter(loglevel=0):
    return """// parse() Generic Zigbee-device footer BEGINS here
logging("PARSE END-----------------------", """ + str(loglevel) + """)
return cmd
// parse() Generic footer ENDS here"""
