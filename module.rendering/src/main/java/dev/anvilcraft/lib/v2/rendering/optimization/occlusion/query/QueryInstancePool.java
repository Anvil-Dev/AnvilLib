package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.query;

import dev.anvilcraft.lib.v2.rendering.foundation.GpuReusableResourcePool;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class QueryInstancePool extends GpuReusableResourcePool<QueryInstance, QueryInstance.CreationContext> {
    public QueryInstancePool(QueryInstance.CreationContext context) {
        super(2, context);
    }

    @Override
    protected QueryInstance createInstance(QueryInstance.CreationContext context, int i) {
        return QueryInstance.newInstance(context, i);
    }
}
