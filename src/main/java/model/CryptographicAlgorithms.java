package model;

import javax.crypto.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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
     * @return ключевую пару {@code KerPair}
     */
    public static KeyPair generateKeyPair()
    {
        KeyPairGenerator kpg = null;

        try
        {
            kpg = KeyPairGenerator.getInstance("RSA");
        }
        catch (NoSuchAlgorithmException ex)
        {
            try (FileWriter fw = new FileWriter("src/main/resources/logs/.log"))
            {
                fw.write(java.time.LocalDateTime.now().toString());
                fw.write(CryptographicAlgorithms.class.getName() + "\n");
                fw.write(new Exception().getStackTrace()[0].getMethodName() + "\n");
                fw.write(ex.getClass().getName() + "\n");
                fw.write(ex.getMessage() + "\n");
            }
            catch (IOException ex1)
            {
                System.out.println("Файла для логирования не существует");
                System.out.println(ex1.getMessage());
            }
        }

        kpg.initialize(2048);

        return kpg.generateKeyPair();
    }

    /**
     * <h3>Развёртка симметричного ключа</h3>
     *
     * <p>Данный метод развёртывает ключ {@param wrappedKey} с помощью секретного ключа {@code key}</p>
     *
     * @param wrappedKey импортируемый ключ, который будет подвержен развёртке.
     * @param key секретный ключ, с помощью которого будет разворачиваться секретный ключ {@code wrappedKey}.
     *
     * @return Секретный ключ или {@code null}, если входе работы произошла ошибка (см. файл .log).
     */
    public static Key unwrapKey(byte[] wrappedKey, PrivateKey key)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("RSA");

            cipher.init(Cipher.UNWRAP_MODE, key);

            return cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
        }
        catch (NoSuchAlgorithmException|NoSuchPaddingException|InvalidKeyException ex)
        {
            try (FileWriter fw = new FileWriter("src/main/resources/logs/.log"))
            {
                fw.write(java.time.LocalDateTime.now().toString());
                fw.write(CryptographicAlgorithms.class.getName() + "\n");
                fw.write(new Exception().getStackTrace()[0].getMethodName() + "\n");
                fw.write(ex.getClass().getName() + "\n");
                fw.write(ex.getMessage() + "\n");
            }
            catch (IOException ex1)
            {
                System.out.println("Файла для логирования не существует");
                System.out.println(ex1.getMessage());
            }
        }

        return null;
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
     * Или {@code null}, если входе работы произошла ошибка (см. файл .log).
     */
    public static byte[] encrypt(byte[] data, Key key)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.ENCRYPT_MODE, key);

            return cipher.doFinal(data);
        }
        catch (NoSuchAlgorithmException|NoSuchPaddingException|InvalidKeyException|IllegalBlockSizeException|
               BadPaddingException ex)
        {
            try (FileWriter fw = new FileWriter("src/main/resources/logs/.log"))
            {
                fw.write(java.time.LocalDateTime.now().toString());
                fw.write(CryptographicAlgorithms.class.getName() + "\n");
                fw.write(new Exception().getStackTrace()[0].getMethodName() + "\n");
                fw.write(ex.getClass().getName() + "\n");
                fw.write(ex.getMessage() + "\n");
            }
            catch (IOException ex1)
            {
                System.out.println("Файла для логирования не существует");
                System.out.println(ex1.getMessage());
            }
        }

        return null;
    }

    /**
     * <h3>Получение секретного ключа для ЭЦП</h3>
     *
     * <p>Находит в хранилище ключей запись с псевдонимом {@code alias}. Загружает из записи секретный ключ.</p>
     *
     * @param alias псевдоним записи.
     * @param password пароль от хранилища ключей.
     *
     * @return Секретный ключ для ЭЦП.
     *
     * @throws UnrecoverableKeyException если задан не верный пароль {@code password}.
     * @throws NullPointerException если запись с псевдонимом {@code alias} не существует.
     */
    public static PrivateKey getPrivateKey(String alias, String password)
            throws UnrecoverableKeyException, NullPointerException
    {
        KeyStore.PrivateKeyEntry pkEntry = null;

        try
        {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            char[] charsPassword = password.toCharArray();

            final String HOMEPATH = System.getenv("HOMEPATH");

            try (FileInputStream fis = new FileInputStream("C:\\"+HOMEPATH+"\\.keystore"))
            {
                keyStore.load(fis, charsPassword);
            }
            catch (IOException ex)
            {
                if (ex.getCause().getClass() == UnrecoverableKeyException.class)
                {
                    throw new UnrecoverableKeyException();
                }
                else
                {
                    System.out.println("Файла для логирования не существует");
                    System.out.println(ex.getMessage());
                }
            }

            KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(charsPassword);

            pkEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, protectionParameter);

            if (pkEntry == null)
                throw new NullPointerException("Запись с псевдонимом " + alias + " не найдена.");
        }
        catch (UnrecoverableKeyException ex) // чтобы UnrecoverableEntryException не перехватил
        {
            throw ex;
        }
        catch (KeyStoreException|NoSuchAlgorithmException|java.security.cert.CertificateException|
               java.security.UnrecoverableEntryException ex)
        {
            try (FileWriter fw = new FileWriter("src/main/resources/logs/.log"))
            {
                fw.write(java.time.LocalDateTime.now().toString());
                fw.write(CryptographicAlgorithms.class.getName() + "\n");
                fw.write(new Exception().getStackTrace()[0].getMethodName() + "\n");
                fw.write(ex.getClass().getName() + "\n");
                fw.write(ex.getMessage() + "\n");
            }
            catch (IOException ex1)
            {
                System.out.println("Файла для логирования не существует");
                System.out.println(ex1.getMessage());
            }
        }

        return pkEntry.getPrivateKey();
    }
}
