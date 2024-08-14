# HL7v2 Bumblebee
The lib-hl7v2-bumblebee library takes an HL7 v2.x message and transforms it into a JSON tree.

## Overview
The design of this library has the following goals:
- Provide a generic HL7 v.2.x transformer that can work for any message.
- Be customizable for different message types and use cases.
- Reduce the need for a consumer of the output JSON to be an HL7 SME, particularly by avoiding segment-field notation 
in the output.
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
1. HL7 Message Profile

The first configuration file required outlines the HL7 message structure.
It has two main sections: "segmentDefinition", which lists the segments, their relationships, and their cardinality;
and "segmentFields", which gives the details of those field in the included segments that should be included in the 
output.
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

2. HL7 Data Types Definitions

The second configuration file needed by HL7JsonTransformer is one that describes the details of each HL7 data type 
that is referenced in the first configuration file. 
This detail is needed in order to encode into JSON the data elements that are complex, i.e., that have components as 
opposed to a simple string or integer value.

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
The HL7JsonTransformer should be instantiated using the factory method `getTransformerWithResource`,
which takes three parameters: `message`, the HL7 message content; `profileFileName`, the path to the message profile configuration 
file, and `fieldProfileName`, an optional parameter providing the path to the data types configuration file. 
If the last parameter is omitted, the "DefaultFieldsProfileSimple.json" file will be used.

Once instantiated, simply call `transformMessage` to return the output JSON as a String.

```kotlin
val transformer = HL7JsonTransformer.getTransformerWithResource(message,
    "PhinProfile.json",
    "DefaultFieldsProfileSimple.json")
val fullHL7json = transformer.transformMessage()
```
Here is an example of the output produced:
```json
{
	"MSH": {
		"field_separator": "|",
		"encoding_characters": "^~\\&",
		"sending_application": {
			"namespace_id": "CA StarLIMS Stage",
			"universal_id": "2.16.840.1.114222.4.3.3.2.3.2",
			"universal_id_type": "ISO"
		},
		"sending_facility": {
			"namespace_id": "CA PHL VRDL",
			"universal_id": "2.16.840.1.114222.4.1.10765",
			"universal_id_type": "ISO"
		},
		"receiving_application": {
			"namespace_id": "PHLIP-VPD",
			"universal_id": "2.16.840.1.114222.4.3.6.1",
			"universal_id_type": "ISO"
		},
		"receiving_facility": {
			"namespace_id": "NCIRD-VPD",
			"universal_id": "2.16.840.1.114222.4.1.214275",
			"universal_id_type": "ISO"
		},
		"date_time_of_message": "20170227140211.773-0800",
		"message_type": {
			"message_code": "ORU",
			"trigger_event": "R01",
			"universal_id_type": "ORU_R01"
		},
		"message_control_id": "V17T01279-01_9993",
		"processing_id": {
			"processing_id": "T"
		},
		"version_id": {
			"version_id": "2.5.1"
		},
		"message_profile_identifier": [
			{
				"entity_identifier": "PHLabReport-NoAck",
				"namespace_id": "phLabResultsELRv251",
				"universal_id": "2.16.840.1.113883.9.11",
				"universal_id_type": "ISO"
			},
			{
				"entity_identifier": "VPD_Version_1.0",
				"namespace_id": "VPDMsgMapID",
				"universal_id": "2.16.840.1.113883.9.30",
				"universal_id_type": "ISO"
			}
		],
		"children": [
			{
				"SFT": {
					"software_vendor_organization": {
						"organization_name": "CA StarLIMS Stage",
						"assigning_authority": {
							"universal_id": "2.16.840.1.114222.4.3.3.2.3.1",
							"universal_id_type": "ISO"
						}
					},
					"software_certified_version_or_release_number": "10.05",
					"software_product_name": "Cal-LIMS",
					"software_binary_id": "0003"
				}
			},
			{
				"PID": {
					"set_id": "1",
					"patient_identifier_list": [
						{
							"id_number": "123456",
							"assigning_authority": {
								"namespace_id": "CA.PHL",
								"universal_id": "2.16.840.1.114222.4.1.10413",
								"universal_id_type": "ISO"
							},
							"identifier_type_code": "PI"
						}
					],
					"date_time_of_birth": "19930101",
					"patient_address": [
						{
							"state_or_province": "CA",
							"country": "USA",
							"address_type": "P"
						}
					]
				}
			},
			{
				"ORC": {
					"order_control": "RE",
					"placer_order_number": {
						"entity_identifier": "CA-V99999999",
						"universal_id": "2.16.840.1.114222.4.1.10765",
						"universal_id_type": "ISO"
					},
					"filler_order_number": {
						"entity_identifier": "Starlims Lab",
						"namespace_id": "CA StarLIMS Stage",
						"universal_id": "2.16.840.1.114222.4.3.3.2.3.2",
						"universal_id_type": "ISO"
					},
					"ordering_provider": [
						{
							"person_identifier": "0123456",
							"family_name": {
								"surname": "Viral and Rickettsial Disease Laboratory"
							},
							"assigning_authority": {
								"namespace_id": "CLIA",
								"universal_id": "2.16.840.1.113883.4.7",
								"universal_id_type": "ISO"
							},
							"identifier_type_code": "XX"
						}
					],
					"ordering_facility_name": [
						{
							"organization_name": "Viral and Rickettsial Disease Laboratory",
							"organization_name_type_code": "D",
							"assigning_authority": {
								"namespace_id": "CLIA",
								"universal_id": "2.16.840.1.113883.4.7",
								"universal_id_type": "ISO"
							},
							"identifier_type_code": "XX",
							"organization_identifier": "05D0643850"
						}
					],
					"ordering_facility_address": [
						{
							"street_address": "850 Marina Bay Parkway",
							"city": "94804-6403",
							"state_or_province": "CA"
						}
					],
					"ordering_facility_phone_number": [
						{
							"telecommunication_use_code": "WPN",
							"telecommunication_equipment_type": "PH",
							"area_city_code": "510",
							"local_number": "6206275"
						}
					]
				}
			},
			{
				"OBR": {
					"set_id": "1",
					"placer_order_number": {
						"entity_identifier": "CA-V99999999",
						"universal_id": "2.16.840.1.114222.4.1.10765",
						"universal_id_type": "ISO"
					},
					"filler_order_number": {
						"entity_identifier": "Starlims Lab",
						"namespace_id": "CA StarLIMS Stage",
						"universal_id": "2.16.840.1.114222.4.3.3.2.3.2",
						"universal_id_type": "ISO"
					},
					"universal_service_identifier": {
						"identifier": "68991-9",
						"text": "Epidemiologically important info pnl",
						"name_of_coding_system": "LN",
						"alternate_identifier": "11454",
						"alternate_text": "V_RTPCR_Mumps",
						"name_of_alternate_coding_system": "L"
					},
					"observation_date_time": "20170217",
					"ordering_provider": {
						"person_identifier": "0123456",
						"family_name": {
							"surname": "Viral and Rickettsial Disease Laboratory"
						},
						"assigning_authority": {
							"namespace_id": "CLIA",
							"universal_id": "2.16.840.1.113883.4.7",
							"universal_id_type": "ISO"
						},
						"identifier_type_code": "XX"
					},
					"results_report_status_change_date_time": "20170227140211.976-0800",
					"result_status": "F",
					"children": [
						{
							"OBX": {
								"set_id": "1",
								"value_type": "TS",
								"observation_identifier": {
									"identifier": "11368-8",
									"text": "Illness or injury onset date and time",
									"name_of_coding_system": "LN"
								},
								"observation_value": [
									"20170213"
								],
								"observation_result_status": "F",
								"date_time_of_the_observation": "20170217",
								"date_time_of_the_analysis": "20170217",
								"performing_organization_name": {
									"organization_name": "Viral and Rickettsial Disease Laboratory",
									"organization_name_type_code": "L",
									"assigning_authority": {
										"namespace_id": "CLIA",
										"universal_id": "2.16.840.1.113883.4.7",
										"universal_id_type": "ISO"
									},
									"identifier_type_code": "XX",
									"organization_identifier": "05D0643850"
								},
								"performing_organization_address": {
									"street_address": "850 Marina Bay Parkway",
									"city": "Richmond",
									"state_or_province": "CA",
									"postal_code": "94804-6403",
									"country": "USA",
									"address_type": "B"
								}
							}
						}
					]
				}
			}
		]
	}
}
```


### TemplateTransformer
The TemplateTransformer also requires two configuration files, but they are different from those used by 
HL7JsonTransformer. The TemplateTransformer needs a JSON template of the output format 
and a profile that specifies the segment order and relationships 
in the inbound HL7 message.

#### Configuration Files
1. JSON Output Template

The first configuration file needed is the JSON output template.
The output template gives the JSON tree structure of the output and the HL7 path
to the value to encode with each key.

Here is an example JSON output configuration template:
```json
{
  "specimen_id": "SPM[1]-2.2.1",
  "emptyValue": "PID[1]-2",
  "message_profile": "MSH-21.1",
  "message_controller_id": "MSH-10",
  "meta_organization": "MSH-4.1",
  "phl": "MSH-4.2",
  "meta_program": "MSH-6.1",
  "message_status": "OBR[1]-25",
  "race":  "PID-10.2",
  "$$SFT[1]-1": "SFT-4",
  "emptyVar": "NO-Map",
  "OBR": [
    {
      "type": "OBR[1]-> OBX-2",
      "question": {
        "code": "OBR[1]->OBX-3.1",
        "label": "OBR[1]->OBX-3.2"
      },
      "Group": "OBR[1]->OBX-4",
      "answer": "OBR[1]->OBX-5"
    }
  ],
  "Tests": [
  {
    "$$OBR[@2.1='201912_03']->OBX[*]-3.1": "OBR[@2.1='201912_03']->OBX-5"
  }
    ],
  "Multiple": "OBX[@3.1='PLT631||PLT656']-5.1"

}
```
The first thing to note is that the use of this template gives you complete control
over what the output looks like. Unlike the HL7JsonTransformer, the TemplateTransformer 
does not "lock you in" to the HL7 message structure, but gives you the freedom to create
whatever output you want.

The second thing to note is that you can also use HL7 paths to specify the keys in your JSON
output. For example, this syntax under "Tests" tells the TemplateTransformer
to look at each OBX under the OBR where OBR-2.1 has the value "201912_03" and use
the OBX-3.1 value as the key and OBX-5.1 as the value:
```
"$$OBR[@2.1='201912_03']->OBX[*]-3.1": "OBR[@2.1='201912_03']->OBX-5"
```
For further information on the HL7 path syntax, please see the documentation
pertaining to the [HL7-PET library](https://github.com/CDCgov/hl7-pet).

2. HL7 Message Profile 

The second configuration file needed by the TemplateTransformer is a more basic version
of the HL7 message profile we saw earlier that is used by HL7JsonTransformer.
This configuration file only gives the "segmentDefinition" section of that file, outlining
the segment order, parent-child relationships, and cardinality in the inbound message.

For an example, please see above or examine the "BasicProfile.json" sample file
in the src/test/resources folder in this repository.

#### Methods
The TemplateTransformer has two factory methods for instantiation: getTransformerWithResource and
getTransformerWithContent.

The `getTransformerWithResource` method has two parameters: `configFileName` is the path to the JSON template file
and `profileName` is the path to the HL7 message profile configuration file.

The alternative method, `getTransformerWithContent`, expects the contents of the JSON output template
as the `template` parameter and the contents of the HL7 message profile as the `profile` parameter. 

So, the choice
is yours whether the TemplateTransformer loads the files' contents for you, or whether you load the contents
into memory first and then pass the values to the TemplateTransformer.

Once the TemplateTransformer has been instantiated, you call the `transformMessage` method
and pass in the contents of the HL7 input message. This method has an optional
second parameter where you can specify a delimiter with which to concatenate any repeating values.
Omitting this parameter will result in the repeating values being encoded as separate array elements.

Here is an example that uses getTransformerWithResource:
```kotlin
val templateTransformer = TemplateTransformer.getTransformerWithResource("/exampleTemplate.json",
"BasicProfile.json")
val jsonStringOutput = templateTransformer.transformMessage(message)
```
And here is an example of the output produced, using the configuration template shown earlier:
```json
{
	"specimen_id": "214MP000912",
	"emptyValue": null,
	"message_profile": "PHLabReport-NoAck",
	"message_controller_id": "ARLN_GC_DupASTmOBR_ELR",
	"meta_organization": "MD.BALTIMORE.SPHL",
	"phl": "2.16.840.1.114222.4.1.95290",
	"meta_program": "CDC.ARLN.GC",
	"message_status": "F",
	"race": [
		"Native Hawaiian",
		"White"
	],
	"emptyVar": null,
	"Multiple": [
		"MD",
		"KS",
		"SFO-1"
	],
	"Epic Systems Corporation": "7.8.0.0",
	"OBR": [
		{
			"type": "CWE",
			"question": {
				"code": "PLT631",
				"label": "ARLN regional lab"
			},
			"answer": [
				"MD^Maryland^FIPS5_2",
				"KS^Kansas^FIPS_5_2"
			]
		},
		{
			"type": "SN",
			"question": {
				"code": "35659-2",
				"label": "Age at specimen collection"
			},
			"Group": "33",
			"answer": "^7"
		},
		{
			"type": "CWE",
			"question": {
				"code": "PLT630",
				"label": "Gender for GC"
			},
			"answer": "M^Male^HL70001"
		},
		{
			"type": "CWE",
			"question": {
				"code": "PLT656",
				"label": "GC facility"
			},
			"Group": "44",
			"answer": "SFO-1^San Francisco City Clinic (SF/CA)^L"
		},
		{
			"type": "CWE",
			"question": {
				"code": "PLT633",
				"label": "Local jurisdiction PHL for GC"
			},
			"answer": "SFL^San Francisco PHL (CA)^L"
		},
		{
			"type": "ST",
			"question": {
				"code": "PLT638",
				"label": "GISP specimen ID for GC"
			},
			"answer": "SFO-201701-01"
		},
		{
			"type": "ST",
			"question": {
				"code": "PLT639",
				"label": "SURRG specimen ID for GC"
			},
			"answer": "SFO123451678911234"
		}
	],
	"Tests": [
		{
			"36-4": "^0.002"
		},
		{
			"80-2": "^0.002"
		},
		{
			"141-2": ">^4"
		},
		{
			"185-9": "^0.002"
		},
		{
			"267-5": "^2.0"
		},
		{
			"6932-8": "^2"
		},
		{
			"496-0": "^0.03"
		}
	]
}
```

## Build
The project can be built using Maven build tools:

`mvn package -f pom.xml`

Alternatively, a release version will soon be available in Maven Central.

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
