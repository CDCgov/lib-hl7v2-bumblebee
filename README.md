# HL7v2 Bumblebee
The lib-hl7v2-bumblebee library takes an HL7 v2.x message and transforms it into a JSON tree.

## Overview
The design of this library had the following goals:
- Provide a generic HL7 v.2.x transformer that can work for any message.
- Be customizable for different message types and use cases.
- Reduce the need for a consumer of the output JSON to be an HL7 SME, particularly by avoiding segment-field notation in the output.
- Be performant with low memory footprint.

The performance needs are met through the use of high-efficiency Kotlin and Groovy code.
For customization, the library requires configuration files that specify the input and output data structures.

## Usage
The library provides two main classes that allow for two different means of transformation:
HL7JsonTransformer, which will output a JSON tree that mimics the structure of the input HL7 message,
and TemplateTransformer, which will produce output according to the format in the provided JSON template.

### HL7JsonTransformer
The HL7JsonTransformer class requires two configuration files: one to specify the input HL7 message structure 
and another to specify the structure of each HL7 data type. The output is then inferred from the message structure,
generating a JSON tree that uses the name of each field or component as its key. 
This output is a rich descriptive account of the data that was input.

#### Configuration Files
1. HL7 message profile

The first configuration file required outlines the HL7 message structure.
It has two main sections: "segmentDefinition", which lists the segments, their relationships, and their cardinality;
and "segmentFields", which gives the details of those field in the included segments that should be included in the output.
Here is an abbreviated example, which shows the complete "segmentDefinition" section
for an ORU message type and the first five fields of the MSH in "segmentFields":
```json
{
  "segmentDefinition": {
    "MSH": {
      "cardinality": "[1..1]",
      "children": {
        "SFT": {
          "cardinality": "[0..*]"
        },
        "PID": {
          "cardinality": "[1..1]",
          "children": {
            "PD1": {
              "cardinality": "[0..1]"
            },
            "NTE": {
              "cardinality": "[0..*]"
            },
            "NK1": {
              "cardinality": "[0..*]"
            },
            "PV1": {
              "cardinality": "[1..1]"
            },
            "PV2": {
              "cardinality": "[0..1]"
            }
          }
        },
        "ORC": {
          "cardinality": "[0..1]"
        },
        "OBR": {
          "cardinality": "[1..*]",
          "children": {
            "TQ1": {
              "cardinality": "[0..1]"
            },
            "OBX": {
              "cardinality": "[0..*]",
              "children": {
                "NTE": {
                  "cardinality": "[0..*]"
                }
              }
            },
            "SPM": {
              "cardinality": "[0..*]",
              "children": {
                "NTE": {
                  "cardinality": "[0..*]"
                }
              }
            },
            "NTE": {
              "cardinality": "[0..*]"
            }
          }
        }
      }
    }
  },
  "segmentFields": {
    "MSH":  [
      {
        "fieldNumber": 1,
        "name": "File Separator",
        "dataType": "ST",
        "maxLength": "1",
        "usage": "R",
        "cardinality": "[1..1]",
        "conformance": "|",
        "notes": ""
      },
      {
        "fieldNumber": 2,
        "name": "Encoding Characters",
        "dataType": "ST",
        "maxLength": "4",
        "usage": "R",
        "cardinality": "[1..1]",
        "conformance": "^~\\&",
        "notes": ""
      },
      {
        "fieldNumber": 3,
        "name": "Sending Application",
        "dataType": "HD",
        "maxLength": "227",
        "usage": "O",
        "cardinality": "[1..1]",
        "conformance": "",
        "notes": ""
      },
      {
        "fieldNumber": 4,
        "name": "Sending Facility",
        "dataType": "HD",
        "maxLength": "227",
        "usage": "O",
        "cardinality": "[1..1]",
        "conformance": "",
        "notes": ""
      },
      {
        "fieldNumber": 5,
        "name": "Receiving Application",
        "dataType": "HD",
        "maxLength": "227",
        "usage": "O",
        "cardinality": "[1..1]",
        "conformance": "",
        "notes": ""
      },
   ...
```
For each of the segments listed in segmentDefinition, the entries in segmentFields delineate which fields 
should be output. Each entry includes the following data points:
- *fieldNumber*: the position of the field within the segment.
- *name*: the name that should appear as the key in the output. The name will be
normalized to lowercase and spaces will be replaced with underscores.
- *dataType*: the code used here should correspond to the data type whose details are defined
in the second configuration file.
- *maxLength*: the maximum length of the value. Reserved for future use.
- *usage*: the HL7 usage code describing whether this field is required ("R", "O", "RE", etc.).
- *cardinality*: defines whether this is a single-value field or, 
if the second number is greater than 1 or is * (unlimited), a multi-value field. 
Multi-value fields appear as arrays in the output.
- *conformance*: reserved for future use and may be empty
- *notes*: any notes to the implementer or future developers that may be helpful can 
be included here.  
 
Full sample configuration files can be found in the src/test/resources folder in this repository.

2. HL7 data types

The second configuration file needed by HL7JsonTransformer is one that describes the details of each HL7 data type that is referenced in the first configuration file. 
This detail is needed in order to encode into JSON the data elements that are complex, i.e., that have components as opposed to a simple string or integer value.

This file contains a single "segmentFields" node with a child for each data type. 
The details included for each component of the data type are the same as those listed above
for the fields, but in this case, "fieldNumber" refers to the component position. 
An example of the "HD" data type definition is shown below.
This example is drawn from the file "DefaultFieldsProfileSimple.json", which is the 
default configuration file if none is provided and can be located in src/test/resources in this repository:
```json
{
    "segmentFields": {
        "HD" : [
            {
                "fieldNumber": 1,
                "name": "Namespace ID",
                "dataType": "IS",
                "maxLength": "20",
                "usage": "RE",
                "cardinality": "[0..1]",
                "conformance": "",
                "notes": ""
            },
            {
                "fieldNumber": 2,
                "name": "Universal ID",
                "dataType": "ST",
                "maxLength": "199",
                "usage": "R",
                "cardinality": "[1..1]",
                "conformance": "REGEX:<<OID>>",
                "notes": ""
            },
            {
                "fieldNumber": 3,
                "name": "Universal ID Type",
                "dataType": "ID",
                "maxLength": "6",
                "usage": "R",
                "cardinality": "[1..1]",
                "conformance": "ISO",
                "notes": ""
            }
        ],
        "MSG": [
            {
                "fieldNumber": 1,
           ...
```

#### Methods
The HL7JsonTransformer should be instantiated using the factory method "getTransformerWithResource",
which takes three parameters: the HL7 message (String), the path to the message profile configuration 
file (String), and the path to the data types configuration file (String).

Once instantiated, simply call "transformMessage()" to return the output JSON.

```kotlin
        val transformer = HL7JsonTransformer.getTransformerWithResource(message,
            "PhinProfile.json",
            "DefaultFieldsProfileSimple.json")
        val fullHL7 = transformer.transformMessage()
```

### TemplateTransformer
The TemplateTransformer also requires two configuration files, but they are different from those used by 
HL7JsonTransformer. The TemplateTransformer needs a profile that specifies the segment order and relationships 
in the inbound message and a JSON template of the output format.

#### Configuration Files
HL7 message profile
JSON output template

#### Methods



## Dependencies
This library has a dependency on another CDC DEX HL7 library, hl7-pet. The hl7-pet library is available through Maven Central or the public GitHub repository at https://github.com/CDCgov/hl7-pet.


## Related documents

* [Open Practices](open_practices.md)
* [Rules of Behavior](rules_of_behavior.md)
* [Disclaimer](DISCLAIMER.md)
* [Contribution Notice](CONTRIBUTING.md)
* [Code of Conduct](code-of-conduct.md)
  
## Public Domain Standard Notice
This repository constitutes a work of the United States Government and is not
subject to domestic copyright protection under 17 USC § 105. This repository is in
the public domain within the United States, and copyright and related rights in
the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
All contributions to this repository will be released under the CC0 dedication. By
submitting a pull request you are agreeing to comply with this waiver of
copyright interest.

## License Standard Notice
The repository utilizes code licensed under the terms of the Apache Software
License and therefore is licensed under ASL v2 or later.

This source code in this repository is free: you can redistribute it and/or modify it under
the terms of the Apache Software License version 2, or (at your option) any
later version.

This source code in this repository is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the Apache Software License for more details.

You should have received a copy of the Apache Software License along with this
program. If not, see http://www.apache.org/licenses/LICENSE-2.0.html

The source code forked from other open source projects will inherit its license.

## Privacy Standard Notice
This repository contains only non-sensitive, publicly available data and
information. All material and community participation is covered by the
[Disclaimer](DISCLAIMER.md)
and [Code of Conduct](code-of-conduct.md).
For more information about CDC's privacy policy, please visit [http://www.cdc.gov/other/privacy.html](https://www.cdc.gov/other/privacy.html).

## Contributing Standard Notice
Anyone is encouraged to contribute to the repository by [forking](https://help.github.com/articles/fork-a-repo)
and submitting a pull request. (If you are new to GitHub, you might start with a
[basic tutorial](https://help.github.com/articles/set-up-git).) By contributing
to this project, you grant a world-wide, royalty-free, perpetual, irrevocable,
non-exclusive, transferable license to all users under the terms of the
[Apache Software License v2](http://www.apache.org/licenses/LICENSE-2.0.html) or
later.

All comments, messages, pull requests, and other submissions received through
CDC including this GitHub page may be subject to applicable federal law, including but not limited to the Federal Records Act, and may be archived. Learn more at [http://www.cdc.gov/other/privacy.html](http://www.cdc.gov/other/privacy.html).

## Records Management Standard Notice
This repository is not a source of government records, but is a copy to increase
collaboration and collaborative potential. All government records will be
published through the [CDC web site](http://www.cdc.gov).

## Additional Standard Notices
Please refer to [CDC's Template Repository](https://github.com/CDCgov/template) for more information about [contributing to this repository](https://github.com/CDCgov/template/blob/main/CONTRIBUTING.md), [public domain notices and disclaimers](https://github.com/CDCgov/template/blob/main/DISCLAIMER.md), and [code of conduct](https://github.com/CDCgov/template/blob/main/code-of-conduct.md).
