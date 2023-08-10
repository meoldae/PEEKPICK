package com.vvs.peekpick.peek.service;

import com.vvs.peekpick.entity.Peek;
import com.vvs.peekpick.peek.dto.PeekReactionCntDto;
import com.vvs.peekpick.peek.dto.PeekRedisDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PeekRedisServiceImpl implements PeekRedisService{

    public static final String PEEK_REDIS = "Peek:"; //(key) Peek:peek의 id / (value) Peek
    public static final String PEEK_LOCATION_REDIS = "Peek_Location"; //(key) Peek_Location:peek의 id / (value) Peek의 값

    @Qualifier("peekRedisTemplate")
    private final RedisTemplate<String, Object> peekTemplate;

    private final RedisTemplate<String, String> locationTemplate;
    private GeoOperations<String, String> geoOps;
    private ValueOperations<String, Object> valueOps;
    private SetOperations<String, Object> setOps;

    @PostConstruct
    public void init() {
        geoOps = locationTemplate.opsForGeo();
        valueOps = peekTemplate.opsForValue();
        setOps = peekTemplate.opsForSet();
    }

    public boolean peekExist(Long peekId) {
        if(!peekTemplate.hasKey(PEEK_REDIS + peekId)) {
            log.warn("Peek:{} does not exist.", peekId);
            return false;
        }
        return true;
    }

    @Override
    public void setPeek(PeekRedisDto peekRedisDto, Long peekId, int time) {
        peekTemplate.opsForValue().set(PEEK_REDIS + peekId, peekRedisDto, Duration.ofMinutes(time));
    }

    @Override
    public PeekReactionCntDto deletePeek(Long peekId) {
        try {
            peekTemplate.delete(PEEK_REDIS + peekId);
            geoOps.remove(PEEK_LOCATION_REDIS, String.valueOf(peekId));
            PeekReactionCntDto peekReactionCntDto = PeekReactionCntDto.builder()
                    .likeCnt(setOps.size("Peek_Liked:" + peekId).intValue())
                    .disLikeCnt(setOps.size("Peek_DisLiked:" + peekId).intValue())
                    .build();
            peekTemplate.delete("Peek_Viewed:" + peekId);
            peekTemplate.delete("Peek_Liked:" + peekId);
            peekTemplate.delete("Peek_DisLiked:" + peekId);

            return peekReactionCntDto;
        }
        catch (DataAccessException e) {
            log.error("Error get from Redis: ", e);
            return null;
        }
    }

    @Override
    public PeekRedisDto getPeek(Long peekId) {
        try {
            return (PeekRedisDto) peekTemplate.opsForValue().get(PEEK_REDIS + peekId);
        }
        catch (DataAccessException e) {
            log.error("Error get from Redis: ", e);
            return null;
        }
    }
    @Override
    public void setPeekLocation(double lon, double lat, Long peekId) {
        try {
            geoOps.add(PEEK_LOCATION_REDIS, new Point(lon, lat), peekId.toString());
        } catch (Exception e) {
            log.error("Error while adding to geoOps: ", e);
        }
    }

    @Override
    public List<String> getNearLocation(Point point, double distance) { //GeoResults<RedisGeoCommands.GeoLocation<String>>
        List<String> nearPeekIds = new ArrayList<>();
        GeoResults<RedisGeoCommands.GeoLocation<String>> allLocations = geoOps.radius(PEEK_LOCATION_REDIS, new Circle(new Point(point.getX(), point.getY()), new Distance(distance, RedisGeoCommands.DistanceUnit.KILOMETERS)));
        allLocations.forEach(location -> {
            nearPeekIds.add(location.getContent().getName());
        });
        return nearPeekIds;
    }

    @Override
    public void setPeekValueOps(Long peekId, PeekRedisDto updatedPeekRedisDto) {
        valueOps.set(PEEK_REDIS + peekId, updatedPeekRedisDto);
    }

    @Override
    public PeekRedisDto getPeekValueOps(Long peekId) {
        return (PeekRedisDto) valueOps.get(PEEK_REDIS + peekId);
    }

    @Override
    public void setViewedByMember(Long memberId, Long peekId) {
        if(!peekExist(peekId)) {
            log.warn("Peek:{} does not exist.", peekId);
            return;
        }
        setOps.add("Peek_Viewed:" + peekId, String.valueOf(memberId));
    }
    @Override
    public boolean getViewdByMember(Long memberId, Long peekId) {
        if(!peekTemplate.hasKey(PEEK_REDIS + peekId)) return false;

        return setOps.isMember("Peek_Viewed:" + peekId, String.valueOf(memberId));
        //return setOps.isMember("member:" + memberId + ":viewed", String.valueOf(peekId));
    }

    @Override
    public void setPeekReactionOn(Long memberId, boolean like, Long peekId) {
        if(!peekTemplate.hasKey(PEEK_REDIS + peekId)) return;
        if(like) {
            setOps.add("Peek_Liked:" + peekId, String.valueOf(memberId));
        }
        else {
            setOps.add("Peek_DisLiked:" + peekId, String.valueOf(memberId));
        }
    }

    @Override
    public void setPeekReactionOff(Long memberId, boolean like, Long peekId) {
        if(!peekTemplate.hasKey(PEEK_REDIS + peekId)) return;
        if(like) setOps.remove("Peek_Liked:" + peekId, String.valueOf(memberId));
        else setOps.remove("Peek_DisLiked:" + peekId, String.valueOf(memberId));
    }


    @Override
    public boolean getReactionMember(Long memberId, boolean like, Long peekId) {
        if(!peekTemplate.hasKey(PEEK_REDIS + peekId)) return false;
        if(like) return setOps.isMember("Peek_Liked:" + peekId, String.valueOf(memberId));
        else return setOps.isMember("Peek_DisLiked:" + peekId, String.valueOf(memberId));
    }



}
