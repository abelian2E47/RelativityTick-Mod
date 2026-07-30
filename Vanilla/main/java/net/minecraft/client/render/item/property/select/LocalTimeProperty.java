package net.minecraft.client.render.item.property.select;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class LocalTimeProperty implements SelectProperty<String> {
    public static final String DEFAULT_FORMATTED_TIME = "";
    private static final long MILLIS_PER_SECOND = TimeUnit.SECONDS.toMillis(1L);
    private static final Codec<TimeZone> TIME_ZONE_CODEC = Codec.STRING.comapFlatMap(timeZone -> {
        TimeZone timeZone2 = TimeZone.getTimeZone(timeZone);
        return timeZone2.equals(TimeZone.UNKNOWN_ZONE) ? DataResult.error(() -> "Unknown timezone: " + timeZone) : DataResult.success(timeZone2);
    }, TimeZone::getID);
    private static final MapCodec<LocalTimeProperty.Data> DATA_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
                Codec.STRING.fieldOf("pattern").forGetter(data -> data.format),
                Codec.STRING.optionalFieldOf("locale", "").forGetter(data -> data.localeId),
                TIME_ZONE_CODEC.optionalFieldOf("time_zone").forGetter(data -> data.timeZone)
            )
            .apply(instance, LocalTimeProperty.Data::new)
    );
    public static final SelectProperty.Type<LocalTimeProperty, String> TYPE = SelectProperty.Type.create(
        DATA_CODEC.flatXmap(LocalTimeProperty::validate, property -> DataResult.success(property.data)), Codec.STRING
    );
    private final LocalTimeProperty.Data data;
    private final DateFormat dateFormat;
    private long nextUpdateTime;
    private String currentTimeFormatted = "";

    private LocalTimeProperty(LocalTimeProperty.Data data, DateFormat dateFormat) {
        this.data = data;
        this.dateFormat = dateFormat;
    }

    public static LocalTimeProperty create(String pattern, String locale, Optional<TimeZone> timeZone) {
        return validate(new LocalTimeProperty.Data(pattern, locale, timeZone))
            .getOrThrow(format -> new IllegalStateException("Failed to validate format: " + format));
    }

    private static DataResult<LocalTimeProperty> validate(LocalTimeProperty.Data data) {
        ULocale uLocale = new ULocale(data.localeId);
        Calendar calendar = data.timeZone.<Calendar>map(timeZone -> Calendar.getInstance(timeZone, uLocale)).orElseGet(() -> Calendar.getInstance(uLocale));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(data.format, uLocale);
        simpleDateFormat.setCalendar(calendar);

        try {
            simpleDateFormat.format(new Date());
        } catch (Exception exception) {
            return DataResult.error(() -> "Invalid time format '" + simpleDateFormat + "': " + exception.getMessage());
        }

        return DataResult.success(new LocalTimeProperty(data, simpleDateFormat));
    }

    @Nullable
    public String getValue(
        ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ModelTransformationMode modelTransformationMode
    ) {
        long l = Util.getMeasuringTimeMs();
        if (l > this.nextUpdateTime) {
            this.currentTimeFormatted = this.formatCurrentTime();
            this.nextUpdateTime = l + MILLIS_PER_SECOND;
        }

        return this.currentTimeFormatted;
    }

    private String formatCurrentTime() {
        return this.dateFormat.format(new Date());
    }

    @Override
    public SelectProperty.Type<LocalTimeProperty, String> getType() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    record Data(String format, String localeId, Optional<TimeZone> timeZone) {
    }
}

