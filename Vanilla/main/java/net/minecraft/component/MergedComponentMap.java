package net.minecraft.component;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * A {@link Component Map} that has a base map and changes to be applied on top of it.
 */
public final class MergedComponentMap implements ComponentMap {
    private final ComponentMap baseComponents;
    private Reference2ObjectMap<ComponentType<?>, Optional<?>> changedComponents;
    private boolean copyOnWrite;

    public MergedComponentMap(ComponentMap baseComponents) {
        this(baseComponents, Reference2ObjectMaps.emptyMap(), true);
    }

    private MergedComponentMap(ComponentMap baseComponents, Reference2ObjectMap<ComponentType<?>, Optional<?>> changedComponents, boolean copyOnWrite) {
        this.baseComponents = baseComponents;
        this.changedComponents = changedComponents;
        this.copyOnWrite = copyOnWrite;
    }

    public static MergedComponentMap create(ComponentMap baseComponents, ComponentChanges changes) {
        if (shouldReuseChangesMap(baseComponents, changes.changedComponents)) {
            return new MergedComponentMap(baseComponents, changes.changedComponents, true);
        }

        MergedComponentMap mergedComponentMap = new MergedComponentMap(baseComponents);
        mergedComponentMap.applyChanges(changes);
        return mergedComponentMap;
    }

    private static boolean shouldReuseChangesMap(ComponentMap baseComponents, Reference2ObjectMap<ComponentType<?>, Optional<?>> changedComponents) {
        for (Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(changedComponents)) {
            Object object = baseComponents.get(entry.getKey());
            Optional<?> optional = entry.getValue();
            if (optional.isPresent() && optional.get().equals(object)) {
                return false;
            }

            if (optional.isEmpty() && object == null) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    @Override
    public <T> T get(ComponentType<? extends T> type) {
        Optional<? extends T> optional = (Optional<? extends T>)this.changedComponents.get(type);
        return (T)(optional != null ? optional.orElse(null) : this.baseComponents.get(type));
    }

    public boolean hasChanged(ComponentType<?> type) {
        return this.changedComponents.containsKey(type);
    }

    @Nullable
    public <T> T set(ComponentType<? super T> type, @Nullable T value) {
        this.onWrite();
        T object = this.baseComponents.get((ComponentType<? extends T>)type);
        Optional<T> optional;
        if (Objects.equals(value, object)) {
            optional = (Optional<T>)this.changedComponents.remove(type);
        } else {
            optional = (Optional<T>)this.changedComponents.put(type, Optional.ofNullable(value));
        }

        return optional != null ? optional.orElse(object) : object;
    }

    @Nullable
    public <T> T remove(ComponentType<? extends T> type) {
        this.onWrite();
        T object = this.baseComponents.get(type);
        Optional<? extends T> optional;
        if (object != null) {
            optional = (Optional<? extends T>)this.changedComponents.put(type, Optional.empty());
        } else {
            optional = (Optional<? extends T>)this.changedComponents.remove(type);
        }

        return (T)(optional != null ? optional.orElse(null) : object);
    }

    public void applyChanges(ComponentChanges changes) {
        this.onWrite();

        for (Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(changes.changedComponents)) {
            this.applyChange(entry.getKey(), entry.getValue());
        }
    }

    private void applyChange(ComponentType<?> type, Optional<?> optional) {
        Object object = this.baseComponents.get(type);
        if (optional.isPresent()) {
            if (optional.get().equals(object)) {
                this.changedComponents.remove(type);
            } else {
                this.changedComponents.put(type, optional);
            }
        } else if (object != null) {
            this.changedComponents.put(type, Optional.empty());
        } else {
            this.changedComponents.remove(type);
        }
    }

    public void setChanges(ComponentChanges changes) {
        this.onWrite();
        this.changedComponents.clear();
        this.changedComponents.putAll(changes.changedComponents);
    }

    public void clearChanges() {
        this.onWrite();
        this.changedComponents.clear();
    }

    public void setAll(ComponentMap components) {
        for (Component<?> component : components) {
            component.apply(this);
        }
    }

    private void onWrite() {
        if (this.copyOnWrite) {
            this.changedComponents = new Reference2ObjectArrayMap<>(this.changedComponents);
            this.copyOnWrite = false;
        }
    }

    @Override
    public Set<ComponentType<?>> getTypes() {
        if (this.changedComponents.isEmpty()) {
            return this.baseComponents.getTypes();
        }

        Set<ComponentType<?>> set = new ReferenceArraySet<>(this.baseComponents.getTypes());

        for (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(
            this.changedComponents
        )) {
            Optional<?> optional = entry.getValue();
            if (optional.isPresent()) {
                set.add(entry.getKey());
            } else {
                set.remove(entry.getKey());
            }
        }

        return set;
    }

    @Override
    public Iterator<Component<?>> iterator() {
        if (this.changedComponents.isEmpty()) {
            return this.baseComponents.iterator();
        }

        List<Component<?>> list = new ArrayList<>(this.changedComponents.size() + this.baseComponents.size());

        for (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(
            this.changedComponents
        )) {
            if (entry.getValue().isPresent()) {
                list.add(Component.of(entry.getKey(), entry.getValue().get()));
            }
        }

        for (Component<?> component : this.baseComponents) {
            if (!this.changedComponents.containsKey(component.type())) {
                list.add(component);
            }
        }

        return list.iterator();
    }

    @Override
    public int size() {
        int i = this.baseComponents.size();

        for (it.unimi.dsi.fastutil.objects.Reference2ObjectMap.Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(
            this.changedComponents
        )) {
            boolean bl = entry.getValue().isPresent();
            boolean bl2 = this.baseComponents.contains(entry.getKey());
            if (bl != bl2) {
                i += bl ? 1 : -1;
            }
        }

        return i;
    }

    public ComponentChanges getChanges() {
        if (this.changedComponents.isEmpty()) {
            return ComponentChanges.EMPTY;
        }

        this.copyOnWrite = true;
        return new ComponentChanges(this.changedComponents);
    }

    public MergedComponentMap copy() {
        this.copyOnWrite = true;
        return new MergedComponentMap(this.baseComponents, this.changedComponents, true);
    }

    public ComponentMap immutableCopy() {
        return this.changedComponents.isEmpty() ? this.baseComponents : this.copy();
    }

    @Override
    public boolean equals(Object o) {
        return this == o
            ? true
            : o instanceof MergedComponentMap mergedComponentMap
                && this.baseComponents.equals(mergedComponentMap.baseComponents)
                && this.changedComponents.equals(mergedComponentMap.changedComponents);
    }

    @Override
    public int hashCode() {
        return this.baseComponents.hashCode() + this.changedComponents.hashCode() * 31;
    }

    @Override
    public String toString() {
        return "{" + this.stream().map(Component::toString).collect(Collectors.joining(", ")) + "}";
    }
}

