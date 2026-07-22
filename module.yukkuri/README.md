# AnvilLib Yukkuri

Yukkuri is AnvilLib's shared large-cauldron vaporization runtime. It does not depend on AnvilCraft: a cauldron
implementation supplies the small `VaporizationCauldron` contract and calls `VaporizationManager.tick` from its
server tick. Current AnvilCraft large cauldrons provide that integration directly, without a Mixin.

Addons register a `VaporizationSource` to describe how a cauldron is heated and expose an `IVaporConsumer` through
`YukkuriCapabilities.VAPOR_CONSUMER` on the block directly above the cauldron. Consumers are open by default: vapor
they cannot accept escapes into the atmosphere, and a missing consumer also allows vaporization to continue. An
airtight consumer can override `sealsOutlet` so its remaining capacity applies backpressure before liquid is drained.

## Dependency

The aggregate `anvillib-neoforge-1.21.1` artifact already embeds this module. Addons that depend on the complete
AnvilLib artifact need no additional runtime file. Projects importing only selected AnvilLib modules can use:

```groovy
implementation "dev.anvilcraft.lib:anvillib-yukkuri-neoforge-1.21.1:2.0.0"
```

## Registering a consumer

Register `YukkuriCapabilities.VAPOR_CONSUMER` during `RegisterCapabilitiesEvent`. The queried side is
`Direction.DOWN`, and the provider must be located on the first block directly above the large cauldron's top-center
part. `receiveVapor` must honor `VaporAction.SIMULATE` without changing state. Open consumers receive only their
simulated accepted amount during execution; the rest is vented.

## Registering a source

Register one `VaporizationSource` from mod initialization:

```java
VaporizationSources.register(MyVaporizationSource.INSTANCE);
```

`createOffer` discovers the source structure and returns an exact fluid-input/vapor-output pair without changing the
world. `commit` consumes source-specific fuel or power after the cauldron input has been drained. Sources are checked
by descending priority and then by resource-location ID; the first source that completes a transaction wins the tick.

Standard vapor IDs remain `yukkuri:gaseous_oil` and `yukkuri:gaseous_water` so existing recipes and saved data do not
change when moving the runtime into AnvilLib.
