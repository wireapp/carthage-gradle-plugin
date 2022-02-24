# carthage-gradle-plugin

Adds support for integrating iOS Carthage dependencies into a KMM project.

## Usage

1. Install the [Carthage build tool](https://github.com/Carthage/Carthage).
2. Create a `Cartfile` with the desired dependencies in the project root.
3. Add a `carthage` entry for your iOS target in the gradle file:

```
ios() {
   carthage {
      dependency("AFNetworking")
   }
}

```
4. The dependency will exposed as `carthage.AFNetworking` by default.
