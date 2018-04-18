package com.example.hendratay.whatheweather.data.entity.mapper

import com.example.hendratay.whatheweather.data.entity.WeatherEntity
import com.example.hendratay.whatheweather.domain.model.Weather

class WeatherMapper: Mapper<WeatherEntity, Weather> {

    override fun mapFromEntity(type: WeatherEntity): Weather {
        return Weather(type.id, type.main, type.description, type.icon)
    }

    override fun mapToEntity(type: Weather): WeatherEntity {
        return WeatherEntity(type.id, type.main, type.description, type.icon)
    }

    fun mapFromEntity(list: List<WeatherEntity>): List<Weather> {
        return list.map { mapFromEntity(it) }
    }

    fun mapToEntity(list: List<Weather>): List<WeatherEntity> {
        return list.map { mapToEntity(it) }
    }

}