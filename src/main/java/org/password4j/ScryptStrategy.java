package org.password4j;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import com.lambdaworks.codec.Base64;
import com.lambdaworks.crypto.SCrypt;
import com.lambdaworks.crypto.SCryptUtil;


public class ScryptStrategy implements HashingStrategy
{
    private int resources = 8; // r

    private int workFactor = 2 << 14; // N

    private int parallelization = 1; // p

    private long requiredBytes = 128L * workFactor * resources * parallelization;

    public ScryptStrategy()
    {
        //
    }

    public ScryptStrategy(int resources, int workFactor, int parallelization)
    {
        this();
        this.resources = resources;
        this.workFactor = workFactor;
        this.parallelization = parallelization;
    }

    @Override
    public Hash hash(char[] plain)
    {
        return hash(plain, SaltGenerator.generate());
    }

    @Override
    public Hash hash(char[] plain, byte[] salt)
    {
        try
        {
            byte[] derived = SCrypt.scrypt(new String(plain).getBytes(), salt, workFactor, resources, parallelization, 32);
            String params = Long.toString((long) (log2(workFactor) << 16 | resources << 8 | parallelization), 16);
            StringBuilder sb = new StringBuilder((salt.length + derived.length) * 2);
            sb.append("$s0$").append(params).append('$');
            sb.append(Base64.encode(salt)).append('$');
            sb.append(Base64.encode(derived));
            return new Hash(this, sb.toString().getBytes(), salt);
        }
        catch (IllegalArgumentException | GeneralSecurityException e)
        {
            String message = "Invalid specification with salt=" + Arrays
                    .toString(salt) + ", N=`" + workFactor + ", r=`" + resources + " and p=`" + parallelization + "`";
            throw new BadParametersException(message, e);
        }
    }

    @Override
    public boolean check(char[] plain, byte[] hashed)
    {
        return SCryptUtil.check(new String(plain), new String(hashed));
    }

    @Override
    public boolean check(char[] plain, byte[] hashed, byte[] salt)
    {
        return check(plain, hashed);
    }

    public long getRequiredBytes()
    {
        return requiredBytes;
    }

    private static int log2(int n)
    {
        int log = 0;
        if ((n & -65536) != 0)
        {
            n >>>= 16;
            log = 16;
        }
        if (n >= 256)
        {
            n >>>= 8;
            log += 8;
        }
        if (n >= 16)
        {
            n >>>= 4;
            log += 4;
        }
        if (n >= 4)
        {
            n >>>= 2;
            log += 2;
        }
        return log + (n >>> 1);
    }
}