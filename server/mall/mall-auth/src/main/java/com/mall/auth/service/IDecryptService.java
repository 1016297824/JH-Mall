package com.mall.auth.service;

/**
 * C 端解密服务接口
 *
 * <p>提供 AES-256-GCM 解密能力，用于解密前端加密传输的敏感数据（如手机号）</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
public interface IDecryptService {

    /**
     * 解密加密数据
     *
     * @param encryptedData Base64 编码的加密数据
     * @return 解密后的明文
     */
    String decrypt(String encryptedData);
}
