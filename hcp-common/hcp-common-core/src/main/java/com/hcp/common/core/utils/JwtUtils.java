package com.hcp.common.core.utils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.crypto.SecretKey;
import com.hcp.common.core.constant.SecurityConstants;
import com.hcp.common.core.text.Convert;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Jwt工具类
 *
 * @author vctgo
 */
public class JwtUtils
{
    /**
     * 解析签名密钥来源：运行环境变量 JWT_SECRET（docker-compose 已注入 .env 中的随机强密钥）。
     * 不再使用硬编码弱口令；缺失或为空时在实际签发/校验时抛出明确异常。
     */
    private static String resolveSecret()
    {
        String s = System.getenv("JWT_SECRET");
        if (s == null)
        {
            s = System.getProperty("JWT_SECRET");
        }
        if (s == null || s.trim().isEmpty())
        {
            throw new IllegalStateException(
                    "JWT_SECRET 未配置：请在 .env 中设置 openssl rand -base64 32 生成的随机强密钥，并确保所有服务都注入了该变量");
        }
        return s;
    }

    /**
     * 由 JWT_SECRET 派生 HMAC 签名密钥。JWT_SECRET 约定为 openssl rand -base64 32 的输出，
     * 按 base64 解码为 32 字节，满足 HS256 最小密钥长度要求；非 base64 时回退为 UTF-8 字节。
     */
    private static SecretKey getSigningKey()
    {
        String secret = resolveSecret();
        byte[] keyBytes;
        try
        {
            keyBytes = Decoders.BASE64.decode(secret);
        }
        catch (Exception e)
        {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    public static String createToken(Map<String, Object> claims)
    {
        return Jwts.builder()
                .setClaims(claims)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    public static Claims parseToken(String token)
    {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 根据令牌获取用户标识
     */
    public static String getUserKey(Claims claims)
    {
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    public static String getUserKey(String token)
    {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.USER_KEY);
    }

    /**
     * 根据令牌获取部门标识
     */
    public static String getDeptId(String token)
    {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.DEPT_ID);
    }

    /**
     * 根据令牌获取用户ID
     */
    public static String getUserId(String token)
    {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.DETAILS);
    }

    /**
     * 根据令牌获取用户名
     */
    public static String getUserName(String token)
    {
        Claims claims = parseToken(token);
        return getValue(claims, SecurityConstants.USERNAME);
    }

    public static String getValue(Claims claims, String key)
    {
        return Convert.toStr(claims.get(key));
    }
}
