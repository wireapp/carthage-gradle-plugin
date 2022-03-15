# carthage-gradle-plugin

[![Wire logo](https://github.com/wireapp/wire/blob/master/assets/header-small.png?raw=true)](https://wire.com/jobs/)

This repository is part of the source code of Wire. You can find more information at [wire.com](https://wire.com) or by contacting opensource@wire.com.

You can find the published source code at [github.com/wireapp/wire](https://github.com/wireapp/wire).

For licensing information, see the attached LICENSE file and the list of third-party licenses at [wire.com/legal/licenses/](https://wire.com/legal/licenses/).

No license is granted to the Wire trademark and its associated logos, all of which will continue to be owned exclusively by Wire Swiss GmbH. Any use of the Wire trademark and/or its associated logos is expressly prohibited without the express prior written consent of Wire Swiss GmbH.

## Description

Adds support for integrating [Carthage](https://github.com/Carthage/Carthage) dependencies into a KMM project.

## Usage

1. Install the [Carthage build tool](https://github.com/Carthage/Carthage).
2. Create a `Cartfile` with the desired dependencies in the project root.
3. Add a `carthage` entry for your target in the gradle file, for example:

```
ios() {
   carthage {
      dependency("AFNetworking")
   }
}

```
4. The dependency will be exposed as `carthage.AFNetworking` by default.

## Publishing to [wire-maven](https://github.com/wireapp/wire-maven)

To publish the plugin for internal consumption (e.g. in [kalium](https://github.com/wireapp/wire-maven)),
checkout [wire-maven](https://github.com/wireapp/wire-maven) side by side with this repository:

```
.
├─ carthage-gradle-plugin/
├─ wire-maven/
```

then:

- create a new branch in wire-maven with pattern `chore/carthage-gradle-plugin/vX.Y.Z`
- run the `publish` task in `carthage-gradle-plugin` to generate the plugin's `.jar`
- commit with message following the pattern `chore: add carthage-gradle-plugin vX.Y.Z`
- create a pull request in wire-maven 

Once the resulting pull request is merged in wire-maven a new version will be avalable for consumption.
