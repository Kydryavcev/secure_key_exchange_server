package model;

import javax.crypto.*;
import java.security.*;

/**
 * <h2>Класс криптографических алгоритмов</h2>
 *
 * <p>В этом классе реализованы криптоалгоритмы, необходимые для создания защищенного соединения и шифрования сообщения.</p>
 *
 * @author Kydryavcev Ilya
 * @version 1.0
 * @since 12.03.24
 */
public class CryptographicAlgorithms
{

    /**
     * <h3>Генерирование ключевой пары для алгоритма RSA</h3>
     *
     * @throws NoSuchAlgorithmException если не один провайдер не поддерживает данный алгоритм (RSA)
     *
     * @return ключевую пару {@code KerPair}
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException
    {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");

        kpg.initialize(2048);

        return kpg.generateKeyPair();
    }

    /**
     * <h3>Развёртка симметричного ключа</h3>
     *
     * <p>Данный метод развёртывает ключ {@param wrappedKey} с помощью секретного ключа {@code key}</p>
     *
     * @throws NoSuchAlgorithmException если преобразование имеет значение null, пусто, имеет недопустимый формат или
     * если ни один поставщик не поддерживает реализацию CipherSpi для указанного алгоритма.
     * @throws NoSuchPaddingException если преобразование содержит схему заполнения, которая недоступна.
     * @throws InvalidKeyException если данный ключ не подходит для инициализации этого шифра или требует параметров
     * алгоритма, которые не могут быть определены из данного ключа, или если данный ключ имеет размер ключа, который
     * превышает максимально допустимый размер ключа (как определено из настроенных файлов политики юрисдикции).
     *
     * @param wrappedKey импортируемый ключ, который будет подвержен развёртке.
     * @param key секретный ключ, с помощью которого будет разворачиваться секретный ключ {@code wrappedKey}.
     *
     * @return секретный ключ.
     */
    public static Key unwrapKey(byte[] wrappedKey, PrivateKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException
    {
        Cipher cipher = Cipher.getInstance("RSA");

        cipher.init(Cipher.UNWRAP_MODE, key);

        return cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
    }

    /**
     * <h3>Зашифрование данных</h3>
     *
     * <p>Шифрует данные представленные параметром {@code data}, ключом {@code key}. Шифрование происходит по алгоритму
     * AES.</p>
     *
     * @param data массив байтов, предсталяющий данные.
     * @param key ключ, с помощью которого будет произведено шифрование.
     *
     * @return Массив байтов, представляющий сабой зашифрованные данные.
     *
     * @throws NoSuchAlgorithmException если преобразование имеет нулевое значение, пусто, имеет недопустимый формат или,
     * если ни один провайдер не поддерживает реализацию CipherSpi для указанного алгоритма.
     * @throws NoSuchPaddingException если преобразование содержит схему заполнения, которая недоступна.
     * @throws InvalidKeyException если данный ключ не подходит для инициализации этого шифра или, если данный ключ имеет
     * размер ключа, который превышает максимально допустимый размер ключа.
     * @throws IllegalBlockSizeException если этот шифр является блочным, заполнение не запрашивалось (только в режиме
     * шифрования), а общая входная длина данных, обработанных этим шифром, не кратна размеру блока; или если этот
     * алгоритм шифрования не может обработать предоставленные входные данные.
     * @throws BadPaddingException если при расшифровании с отсечением дополнительных байтов содержимое дополнительного
     * байта не соответствует количеству байтов, подлежащих отсечению
     */
    public static byte[] encrypt(byte[] data, Key key)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException
    {
        System.out.println(new String(data));
        Cipher cipher = Cipher.getInstance("AES");

        cipher.init(Cipher.ENCRYPT_MODE, key);

        return cipher.doFinal(data);
    }
}
