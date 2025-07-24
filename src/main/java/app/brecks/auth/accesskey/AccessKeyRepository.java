package app.brecks.auth.accesskey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class AccessKeyRepository implements Map<String, AccessKey> {
    private static final Logger logger = LogManager.getLogger();
    private final DataSource dataSource;
    private final ObjectMapper mapper;

    @Autowired
    public AccessKeyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        this.mapper = new ObjectMapper();
    }

    @Override
    public int size() {
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select count(*) from AccessKeys;");
             ResultSet r = p.executeQuery()
        ) {
            if (r.next()) return r.getInt(0);
            else throw new RuntimeException("Unable to get access key count from database");
        } catch (SQLException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) throw new NullPointerException();

        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select 1 from AccessKeys where hash = ?;");
        ) {
            String hash = (String) key;
            p.setString(1, hash);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) throw new NullPointerException();

        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select 1 from AccessKeys where hash = ?;");
        ) {
            AccessKey accessKey = (AccessKey) value;
            p.setString(1, accessKey.hash());
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public AccessKey get(Object key) {
        if (key == null) throw new NullPointerException();

        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select * from AccessKeys where hash = ?;");
        ) {
            String hash = (String) key;
            p.setString(1, hash);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    String email = r.getString("email");
                    KeyDuration duration = KeyDuration.valueOf(r.getString("duration"));
                    HashSet<String> scope = new HashSet<>(Set.of(mapper.readValue(r.getString("scope"), String[].class)));
                    LocalDateTime expiryTime = LocalDateTime.ofEpochSecond(r.getLong("expiryTime"), 0, ZoneOffset.UTC);
                    return new AccessKey(email, duration, scope, expiryTime, hash);
                } else return null;
            }
        } catch (SQLException | JsonProcessingException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public AccessKey put(String key, AccessKey value) {
        if (key == null) throw new NullPointerException();
        if (value == null) throw new NullPointerException();

        AccessKey oldKey = this.get(key);

        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("insert into AccessKeys (hash, email, duration, scope, expiryTime) value (?, ?, ?, ?, ?) as new on duplicate key update email = new.email, duration = new.duration, scope = new.scope, expiryTime = new.expiryTime;");
        ) {
            p.setString(1, key);
            p.setString(2, value.email());
            p.setString(3, value.duration().name());
            p.setString(4, mapper.writeValueAsString(value.scope().toArray(new String[]{})));
            p.setLong(5, value.expiryTime().toEpochSecond(ZoneOffset.UTC));
            p.executeUpdate();
            return oldKey;
        } catch (SQLException | JsonProcessingException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public AccessKey remove(Object key) {
        if (key == null) throw new NullPointerException();

        AccessKey oldKey = this.get(key);

        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("delete from AccessKeys where hash = ?");
        ) {
            String hash = (String) key;
            p.setString(1, hash);
            p.executeUpdate();
            return oldKey;
        } catch (SQLException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends AccessKey> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("truncate AccessKeys;");
        ) {
            p.executeUpdate();
        } catch (SQLException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select hash from AccessKeys;");
             ResultSet r = p.executeQuery()
        ) {
            Set<String> results = new HashSet<>();
            while (r.next()) results.add(r.getString(1));
            return results;
        } catch (SQLException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public Collection<AccessKey> values() {
        try (Connection c = this.dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("select * from AccessKeys;");
             ResultSet r = p.executeQuery()
        ) {
            Collection<AccessKey> results = new HashSet<>();

            while (r.next()) {
                String email = r.getString("email");
                KeyDuration duration = KeyDuration.valueOf(r.getString("duration"));
                HashSet<String> scope = new HashSet<>(Set.of(mapper.readValue(r.getString("scope"), String[].class)));
                LocalDateTime expiryTime = LocalDateTime.ofEpochSecond(r.getLong("expiryTime"), 0, ZoneOffset.UTC);
                String hash = r.getString("hash");
                results.add(new AccessKey(email, duration, scope, expiryTime, hash));
            }

            return results;
        } catch (SQLException | JsonProcessingException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public Set<Entry<String, AccessKey>> entrySet() {
        return values().stream().parallel().map((key) -> new Entry<String, AccessKey>() {
            @Override
            public String getKey() {
                return key.hash();
            }

            @Override
            public AccessKey getValue() {
                return key;
            }

            @Override
            public AccessKey setValue(AccessKey value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean equals(Object o) {
                return key.equals(o);
            }

            @Override
            public int hashCode() {
                return key.hashCode();
            }
        }).collect(Collectors.toSet());
    }
}
