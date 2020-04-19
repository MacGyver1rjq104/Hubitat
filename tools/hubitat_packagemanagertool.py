#  Copyright 2020 Markus Liljergren
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
  Imports
"""
import requests
import os
import json
import uuid
import logging
from urllib.parse import urlparse
from datetime import date

"""
  Hubitat Package Manager Tool class
  This class generates Manifest for Dominick Meglio's HE Package Manager (https://github.com/dcmeglio/hubitat-packagemanager)
  WARNING: Exceptions are not properly implemented in this code, use at your own risk!
"""

class HubitatPackageManagerTool:
  def __init__(self, author, minimumHEVersionDefault, dateReleasedDefault=None, 
               gitHubUrl=None, payPalUrl=None):
    self.log = logging.getLogger(__name__)
    self.minimumHEVersionDefault = minimumHEVersionDefault
    self.authorDefault = author
    if(dateReleasedDefault==None):
      self.dateReleasedDefault = date.today().strftime('%Y-%m-%d')
    else:
      self.dateReleasedDefault = dateReleasedDefault
    
    self.repository = {
      "author": author,
    }
    if(gitHubUrl != None):
      self.repository['gitHubUrl'] = gitHubUrl
    if(payPalUrl != None):
      self.repository['payPalUrl'] = payPalUrl
    self.repository['packages'] = []
    self.packages = []
    
  def addPackage(self, package, category, location, description):
    package.applyDefaults(minimumHEVersion=self.minimumHEVersionDefault, author=self.authorDefault,
        dateReleased=self.dateReleasedDefault)
    self.packages.append(package)
    self.repository['packages'].append({
      "name": package.manifestDict['packageName'],
      "category": category,
      "location": location,
      "description": description
    })

  def buildRepository(self, output="repository.json"):
    with open(output, 'w') as f:
      f.write(json.dumps(self.repository, indent=2))
  
  def printJSON(self):
    json_formatted_str = json.dumps(self.repository, indent=2)
    print(json_formatted_str)


class HubitatPackageManagerPackage:
  def __init__(self, packageName, minimumHEVersion=None, author=None, dateReleased=None, releaseNotes=None,
               documentationLink=None, communityLink=None):
    self.log = logging.getLogger(__name__)
    self.manifestDict = {
      "packageName": packageName,
      "minimumHEVersion": minimumHEVersion,
      "author": author,
      "dateReleased": dateReleased
    }
    if(releaseNotes != None):
      self.manifestDict['releaseNotes'] = releaseNotes
    if(documentationLink != None):
      self.manifestDict['documentationLink'] = documentationLink
    if(communityLink != None):
      self.manifestDict['communityLink'] = communityLink
    self.manifestDict['apps'] = []
    self.manifestDict['drivers'] = []
    
  def applyDefaults(self, minimumHEVersion=None, author=None, dateReleased=None):
    if(self.manifestDict['minimumHEVersion'] == None):
      if(minimumHEVersion == None):
        self.log.error("minimumHEVersion has to be set!")
      else:
        self.manifestDict['minimumHEVersion'] = minimumHEVersion
    
    if(self.manifestDict['author'] == None):
      if(author == None):
        self.log.error("author has to be set!")
      else:
        self.manifestDict['author'] = author
    
    if(self.manifestDict['dateReleased'] == None):
      if(dateReleased == None):
        self.manifestDict['dateReleased'] = date.today().strftime('%Y-%m-%d')
      else:
        self.manifestDict['dateReleased'] = dateReleased

  def addApp(self, name, version, namespace, location, required, oauth, internalId, id=None):
    newApp = {
      "id" : id,
      "internalId" : internalId,
      "name": name,
      "version": version,
      "namespace": namespace,
      "location": location,
      "required": required,
      "oauth": oauth
    }
    self.manifestDict['apps'].append(newApp)
  
  def clearApps(self):
    self.manifestDict['apps'] = []

  def addDriver(self, name, version, namespace, location, required, internalId, id=None):
    newDriver = {
      "id" : id,
      "internalId" : internalId,
      "name": name,
      "version": version,
      "namespace": namespace,
      "location": location,
      "required": required
    }
    
    self.manifestDict['drivers'].append(newDriver)
  
  def buildManifest(self, output="packageManifest.json"):
    # First check if it already exists and have IDs set
    if(os.path.isfile(output) and os.access(output, os.R_OK)):
      mdata = {}
      with open(output, 'r') as f:
        mdata = json.load(f)
      if('drivers' in mdata):
        drivers_dict = {}
        for d in mdata['drivers']:
          drivers_dict[d['internalId']] = d
        for d in self.manifestDict['drivers']:
          if(d['internalId'] in drivers_dict and drivers_dict[d['internalId']]['id'] != None):
            d['id'] = drivers_dict[d['internalId']]['id']
            #print('Found this Driver ID: ' + drivers_dict[d['internalId']]['id'])

      if('apps' in mdata):
        apps_dict = {}
        for d in mdata['apps']:
          apps_dict[d['internalId']] = d
        for d in self.manifestDict['apps']:
          if(d['internalId'] in apps_dict and apps_dict[d['internalId']]['id'] != None):
            d['id'] = apps_dict[d['internalId']]['id']
            #print('Found this App ID: ' + apps_dict[d['internalId']]['id'])
    
    for d in self.manifestDict['drivers']:
      if(d['id'] == None):
        d['id'] = str(uuid.uuid5(uuid.NAMESPACE_DNS, d['name'] + d['namespace']))
        #print('Made this Driver ID: ' + d['id'])

    for d in self.manifestDict['apps']:
      if(d['id'] == None):
        d['id'] = str(uuid.uuid5(uuid.NAMESPACE_DNS, d['name'] + d['namespace']))
        #print('Made this App ID: ' + d['id'])

    # Then write the update
    with open(output, 'w') as f:
      f.write(json.dumps(self.manifestDict, indent=2))

  def clearDrivers(self):
    self.manifestDict['drivers'] = []
  
  def printJSON(self):
    json_formatted_str = json.dumps(self.manifestDict, indent=2)
    print(json_formatted_str)