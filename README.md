# mybatis_sonar
The original version that I referred is here: https://github.com/donhui/sonar-mybatis

## Introduction

Welcome to mybatis_sonar, an enhanced version of Donhui's SonarQube plugin for MyBatis. This project aims to improve code scanning and rules logic for MyBatis, making it more powerful and efficient.


## Getting Started

The readme can be found from donhui's readme https://github.com/donhui/sonar-mybatis/blob/master/README.md

## Enhancements

- Extended functionality for scanning <SET> functions in <update> statements to detect duplicated sets.
- Compatibility with SonarQube 10.2 (latest).
- Improved rules logic for MyBatis scanning.

### Prerequisites

Sonarqube installed

### Installation

To install the mybatis_sonar plugin, follow these steps:

1. Download the latest release from the [Releases](https://github.com/Kitsune1797/mybatis_sonar/releases) section.
2. Move the downloaded file to `<Sonarqube_location>/extensions/plugins`.
3. Restart the SonarQube server.



## My Enhances walkthrough

**Customizing the MyBatis Plugin**

While exploring SonarQube's capabilities, I stumbled upon multiple limitations, such as: MyBatis could only detect results in the parent `<select>` statement, ignoring the content within the parent `<select>` tags. For example, it can detect <select parameter=”1=1”>, but not <select> SELECT 1=1 </select>.

To address this limitation, I proceed by modifying the existing rules structure into an array format. This transformation aimed to store errors without allowing them to overlap, which is a much promising approach. 

Then, I embarked on a mission to enhance the plugin's functionality. I introduced a new rule to declare that multiple declarations of the same variable were not allowed due to the hidden possibility of significant error that might lead to failure in the code.

In my countless time digging for a solution regarding original mybatis error, I finally realized the issue's root cause: the plugin was struggling when scanning the content of the "update" element. With this realization, I made the decision to revamp the entire logic that makes file scanning possible.

By implementing new scanner logic I had developed, it is now integratable with SonarQube seamlessly. 

## Conclusion

The mybatis_sonar project represents a significant enhancement to Donhui's SonarQube plugin for MyBatis. By addressing limitations, introducing new features, and optimizing logic, this project aims to provide a more robust and efficient tool for code scanning and analysis. We welcome contributions and feedback from the community to further improve this plugin.
