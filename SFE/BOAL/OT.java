// OT.java - Oblivious Transfer handling.
// Copyright (C) 2004 Dahlia Malkhi, Yaron Sella. 
// See full copyright license terms in file ../GPL.txt

package SFE.BOAL;

import org.apache.log4j.*;

import java.io.*;

import java.math.*;

import java.util.*;

public class OT {
    private static final Logger logger = Logger.getLogger(OT.class);
    private static final int ot_type1 = 1;
    private static final int ot_type2 = 2;
    private static final int ot_type3 = 3;
    private static final int ot_type4 = 4;
    private static final int ot_type5 = 5;
    private static final int ot_type6 = 6;
    private static final int ot_type8 = 8;
    private static final int ot_type11 = 11;
    private static final int ot_type12 = 12;
    private static final int ot_type13 = 13;
    private static int ot_type;
    private BigInteger N = MyUtil.modulus; 
    public static final int NBYTESG = 20;

    //---------------------------------------------------------------
    public OT(int ot_type) {
        if ((ot_type != ot_type1) && (ot_type != ot_type2) &&
		    (ot_type != ot_type3) && (ot_type != ot_type4) &&
		    (ot_type != ot_type5) && (ot_type != ot_type6) &&
		    (ot_type != ot_type8) && (ot_type != ot_type11) &&
		    (ot_type != ot_type12)&& (ot_type != ot_type13))
        {
            logger.error("OT:Unsupported ot_type: " + ot_type);
            System.exit(-1);
        } else {
        	OT.ot_type = ot_type;
        }
    }

    //---------------------------------------------------------------

    /**
     * This routine prepares a vector of OTs for the chooser,
     * executes them, and places their results in the circuit
     *
     * @param c a gate-level circuit
     * @param f I/O format for the circuit
     * @param ot controls what type of OT to do
     * @param oos for outgoing communication
     * @param ois for incoming communication
     */
    public static void ChooserOTs(Circuit c, Formatter f, OT ot,
        ObjectOutputStream oos, ObjectInputStream ois)
    {
        Vector<OTTASK> OTs = new Vector<OTTASK>(10, 10);
        IO io = null;

        // Construct vector of all OTs to be executed
        for (int i = 0; i < f.FMT.size(); i++) {
            io = f.FMT.elementAt(i);

            // Only chooser's (= Alice) inputs are interesting here
            if ((!io.isInput()) || (!io.isAlice())) {
                continue;
            }

            // Gather all required OTs in a vector of OTTASKs
            for (int j = 0; j < io.getNLines(); j++) {
                // Get all the params required for making an OTTASK
                int line_num = io.getLinenum(j);
                Gate g = c.getGate(line_num);
                int b = g.getValue();

                // Make an OTTASK and add it to the vector
                OTTASK ottask = new OTTASK(line_num, 2, b);
                OTs.add(ottask);
            }
        }

        // Execute all the required OTs
        ot.executeChooserOTs(OTs, oos, ois);

        // Place results of all OTs in circuit
        for (int i = 0; i < OTs.size(); i++) {
            OTTASK ottask = OTs.elementAt(i);
            Gate g = c.getGate(ottask.ot_id);
            g.setPackedCode(ottask.transferred_value);
        }
    }

    //---------------------------------------------------------------

    /**
     * This routine prepares a vector of OTs for the sender,
     * and executes them.
     *
     * @param c a gate-level circuit
     * @param f I/O format for the circuit
     * @param ot controls what type of OT to do
     * @param oos for outgoing communication
     * @param ois for incoming communication
     */
    public static void SenderOTs(Circuit c, Formatter f, OT ot,
        ObjectOutputStream oos, ObjectInputStream ois)
    {
        Vector<OTTASK> OTs = new Vector<OTTASK>(10, 10);
        IO io = null;

        // Construct vector of all OTs to be executed
        for (int i = 0; i < f.FMT.size(); i++) {
            io = f.FMT.elementAt(i);

            // Only chooser's (= Alice) inputs are interesting here
            if ((!io.isInput()) || (!io.isAlice())) {
                continue;
            }

            // Gather all required OTs in a vector of OTTASKs
            for (int j = 0; j < io.getNLines(); j++) {
                // Get all the params required for making an OTTASK
                int line_num = io.getLinenum(j);
                Gate g = c.getGate(line_num);

                // Make an OTTASK and add it to the vector
                // (All our OTs are 1-out-of-2)
                OTTASK ottask = new OTTASK(line_num, 2);
                ottask.addElement(g.getPackedCode(0));
                ottask.addElement(g.getPackedCode(1));
                OTs.add(ottask);
            }
        }

        // Execute all the required OTs
        ot.executeSenderOTs(OTs, oos, ois);
    }

    //===============================================================

    /**
     * executeChooserOTs:
     *
     * @param v Vector of OT tasks to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
    public void executeChooserOTs(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        switch (ot_type) {
        case ot_type1:
            executeChooserOTs_EG(v, oos, ois);

            break;

        case ot_type2:
            executeChooserOTs_EGBatch(v, oos, ois);

            break;

        case ot_type3:
            executeChooserOTs_EGNP(v, oos, ois);

            break;

        case ot_type4:
            executeChooserOTs_EGNPBatch(v, oos, ois);

            break;

        case ot_type5:
            executeChooserOTs_BOT(v, oos, ois);

            break;

		case ot_type6:
		    executeChooserOTs_BOTBatch(v, oos, ois);
	    
		    break;

        case ot_type8:
            executeChooserOTs_PlainBatch(v, oos, ois);

            break;

        case ot_type11:
            executeChooserOTs_EGOcomBatch(v, oos, ois);

            break;

        case ot_type12:
            executeChooserOTs_EGNPOcomBatch(v, oos, ois);

            break;

        case ot_type13:
            executeChooserOTs_EG_NOC(v, oos, ois);

            break;
	    
        default:
            logger.error("executeChooserOTs: unknown OT type: " + ot_type);
            System.exit(-1);
        }
    }

    //---------------------------------------------------------------

    /**
     * executeSenderOTs:
     *
     * @param v Vector of OT tasks to do
     * @param oos ObjectOutputStream to send messages to Chooser
     * @param ois ObjectOutputStream to receive messages from Chooser
     */
    public void executeSenderOTs(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        switch (ot_type) {
        case ot_type1:
            executeSenderOTs_EG(v, oos, ois);

            break;

        case ot_type2:
            executeSenderOTs_EGBatch(v, oos, ois);

            break;

        case ot_type3:
            executeSenderOTs_EGNP(v, oos, ois);

            break;

        case ot_type4:
            executeSenderOTs_EGNPBatch(v, oos, ois);

            break;

		case ot_type5:
		    executeSenderOTs_BOT(v, oos, ois);
	    
            break;

		case ot_type6:
		    executeSenderOTs_BOTBatch(v, oos, ois);
		    
		    break; 
 
        case ot_type8:
            executeSenderOTs_PlainBatch(v, oos, ois);

            break;

        case ot_type11:
            executeSenderOTs_EGOcomBatch(v, oos, ois);

            break;

        case ot_type12:
            executeSenderOTs_EGNPOcomBatch(v, oos, ois);

            break;

        case ot_type13:
            executeSenderOTs_EG_NOC(v, oos, ois);

            break;
	    
        default:
            logger.error("executeSenderOTs: unknown OT type: " + ot_type);
            System.exit(-1);
        }
    }

    //===============================================================

    /**
     * executeChooserOTs_EG: El-Gamal OT
     *
     * @param v Vector of OT tasks to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
    public void executeChooserOTs_EG(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger x;
        BigInteger pub_key;
        int selected;

        for (int i = 0; i < v.size(); i++) {
            OTTASK ottask = v.elementAt(i);
            selected = ottask.selected;
            x = MyUtil.EG_randExp(); // Generate random exponent x
            pub_key = MyUtil.EG_genPublic(selected, x); // Generate an EG pub key

            OTMESS otmess_out = new OTMESS(pub_key);
            MyUtil.sendOTMESS(oos, otmess_out); // Send EG pub key to sender

            OTMESS otmess_in = MyUtil.receiveOTMESS(ois); // Get sender's response
            byte[] valrec = MyUtil.EG_decrypt(otmess_in.num[2*selected+1],
                    otmess_in.num[2*selected], x); // Decrypt
            logger.debug("executeChooserOTs_EG: OT no. " + i + " received: " +
                MyUtil.toHexString(valrec));
            ottask.setTransValue(valrec);
        }
    }

    //---------------------------------------------------------------

    /**
     * executeSenderOTs_EG: El-Gamal OT
     *
     * @param v Vector of OT task to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
    public void executeSenderOTs_EG(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger r0;
        BigInteger r1;
        BigInteger[] num = new BigInteger[4];
        BigInteger[] pub_key = new BigInteger[2];

        for (int i = 0; i < v.size(); i++) {
            OTTASK ottask = v.elementAt(i);
            OTMESS otmess_in = MyUtil.receiveOTMESS(ois);

		    pub_key[0] = otmess_in.num[0];
		    pub_key[1] =  MyUtil.EG_deduce(pub_key[0]);

            r0 = MyUtil.EG_randExp(); // Generate random exponent r0
            r1 = MyUtil.EG_randExp(); // Generate random exponent r1

            byte[] mess0 = ottask.getElement(0);
            byte[] mess1 = ottask.getElement(1);
            num[0] = MyUtil.EG_encrypt(pub_key[0], r0, mess0); // Encrypt mess0
            num[1] = MyUtil.EG_pow(r0); // g^r0 
            num[2] = MyUtil.EG_encrypt(pub_key[1], r1, mess1); // Encrypt mess1
            num[3] = MyUtil.EG_pow(r1); // g^r1 

            OTMESS otmess_out = new OTMESS(num);
            MyUtil.sendOTMESS(oos, otmess_out); // Send EG encryptions to chooser
            logger.debug("executeSenderOTs_EG: OT no. " + i + " sending: " +
                MyUtil.toHexString(mess0) + " or " + MyUtil.toHexString(mess1));
        }
    }

    //===============================================================

    /**
     * executeChooserOTs_EGBatch: El-Gamal OTs +
     *                            communication batching
     *
     * @param v Vector of OT tasks to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
    public void executeChooserOTs_EGBatch(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger[] x = new BigInteger[v.size()];
        BigInteger[] all_pub_keys = new BigInteger[v.size()];
        OTMESS otmess_in;
        OTTASK ottask;
        int selected;

        // Prepare all EG public keys and secret keys
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);
            selected = ottask.selected;
            x[i] = MyUtil.EG_randExp(); // Generate random exponent x
            all_pub_keys[i] = MyUtil.EG_genPublic(selected, x[i]); // Generate an EG pub key
        }

        // Send all EG public keys to sender in one message
        OTMESS otmess_out = new OTMESS(all_pub_keys);
        MyUtil.sendOTMESS(oos, otmess_out);

        // Get sender's response - again in one message
        otmess_in = MyUtil.receiveOTMESS(ois);

        // Perform all EG decryptions
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);
            selected = ottask.selected;

            byte[] valrec = MyUtil.EG_decrypt(otmess_in.num[4*i+2*selected+1],
                    otmess_in.num[4*i+2*selected], x[i]); // Decrypt
            logger.debug("executeChooserOTs_EGBatch: OT no. " + i +
                " received: " + MyUtil.toHexString(valrec));
            ottask.setTransValue(valrec);
        }
    }

    //---------------------------------------------------------------

    /**
     * executeSenderOTs_EGBatch: El-Gamal OTs +
     *                           communication batching
     *
     * @param v Vector of OT task to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
    public void executeSenderOTs_EGBatch(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger r0;
        BigInteger r1;
        BigInteger[] pub_key = new BigInteger[2];
        BigInteger[] all_encs = new BigInteger[4 * v.size()];
        OTMESS otmess_out;
        OTTASK ottask;

        OTMESS otmess_in = MyUtil.receiveOTMESS(ois); // Get all public keys

        // Perform all EG encryptions
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);

            pub_key[0] = otmess_in.num[i];
            pub_key[1] = MyUtil.EG_deduce(pub_key[0]);

            byte[] mess0 = ottask.getElement(0);
            byte[] mess1 = ottask.getElement(1);

            r0 = MyUtil.EG_randExp(); // Generate random exponent r0
            r1 = MyUtil.EG_randExp(); // Generate random exponent r1

            all_encs[4*i+0] = MyUtil.EG_encrypt(pub_key[0], r0, mess0); // Encrypt mess0
            all_encs[4*i+1] = MyUtil.EG_pow(r0); // g^r0
            all_encs[4*i+2] = MyUtil.EG_encrypt(pub_key[1], r1, mess1); // Encrypt mess1
            all_encs[4*i+3] = MyUtil.EG_pow(r1); // g^r1

            logger.debug("executeSenderOTs_EGBatch: OT no. " + i +
                " preparing: " + MyUtil.toHexString(mess0) + " or " +
                MyUtil.toHexString(mess1));
        }

        // Send all EG encryptions to chooser in one message
        otmess_out = new OTMESS(all_encs);
        MyUtil.sendOTMESS(oos, otmess_out);
    }

    //===============================================================

    /**
     * executeChooserOTs_EGNP: El-Gamal OT + Naor-Pinkas opt
     *
     * @param v Vector of OT tasks to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
 
    public void executeChooserOTs_EGNP(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger x;
        BigInteger gr = null;
        BigInteger pub_key;
        OTMESS otmess_in;
        int selected;

        for (int i = 0; i < v.size(); i++) {
            OTTASK ottask = v.elementAt(i);
            selected = ottask.selected;
            x = MyUtil.EG_randExp(); // Generate random exponent x
            pub_key = MyUtil.EG_genPublic(selected, x); // Generate an EG pub key

            OTMESS otmess_out = new OTMESS(pub_key);
            MyUtil.sendOTMESS(oos, otmess_out); // Send EG pub key to sender
            otmess_in = MyUtil.receiveOTMESS(ois); // Get sender's response

            if (i == 0) {
                gr = otmess_in.num[2];
            }

            byte[] valrec = MyUtil.EG_decrypt(gr, otmess_in.num[selected], x); // Decrypt
            logger.debug("executeChooserOTs_EGNP: OT no. " + i + " received: " +
                MyUtil.toHexString(valrec));
            ottask.setTransValue(valrec);
        }
    }

    //---------------------------------------------------------------

    /**
     * executeSenderOTs_EGNP: El-Gamal OT + Naor-Pinkas opt
     *
     * @param v Vector of OT task to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */

    public void executeSenderOTs_EGNP(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger r = MyUtil.EG_randExp(); // Generate random exponent r once
        BigInteger gr = MyUtil.EG_pow(r); // g^r is common to all OTs
        BigInteger[] num ;
        BigInteger[] pub_key = new BigInteger[2];
        OTMESS otmess_out;

        for (int i = 0; i < v.size(); i++) {
            OTTASK ottask = v.elementAt(i);
            OTMESS otmess_in = MyUtil.receiveOTMESS(ois);

		    pub_key[0] = otmess_in.num[0];
		    pub_key[1] = MyUtil.EG_deduce(pub_key[0]);

            byte[] mess0 = ottask.getElement(0);
            byte[] mess1 = ottask.getElement(1);

            if (i == 0) {
                num = new BigInteger[3];
                num[0] = MyUtil.EG_encrypt(pub_key[0], r, mess0); // Encrypt mess0
                num[1] = MyUtil.EG_encrypt(pub_key[1], r, mess1); // Encrypt mess1
                num[2] = gr;
            } else {
                num = new BigInteger[2];
                num[0] = MyUtil.EG_encrypt(pub_key[0], r, mess0); // Encrypt mess0
                num[1] = MyUtil.EG_encrypt(pub_key[1], r, mess1); // Encrypt mess1
            }

            otmess_out = new OTMESS(num);
            MyUtil.sendOTMESS(oos, otmess_out); // Send EG encryptions to chooser
            logger.debug("executeSenderOTs_EGNP: OT no. " + i + " sending: " +
                MyUtil.toHexString(mess0) + " or " + MyUtil.toHexString(mess1));
        }
    }

    //===============================================================

    /**
     * executeChooserOTs_EGNPBatch: El-Gamal OTs + Naor-Pinkas opt +
     *                              communication batching
     *
     * @param v Vector of OT tasks to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
 
    public void executeChooserOTs_EGNPBatch(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger[] x = new BigInteger[v.size()];
        BigInteger gr;
        BigInteger[] all_pub_keys = new BigInteger[v.size()];
        OTMESS otmess_in;
        OTTASK ottask;
        int selected;

        // Prepare all EG public keys and secret keys
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);
            selected = ottask.selected;
		    x[i] = MyUtil.EG_randExp(); // Generate random exponent x
            all_pub_keys[i] = MyUtil.EG_genPublic(selected, x[i]); // Generate an EG pub key
        }

        // Send all EG public keys to sender in one message
        OTMESS otmess_out = new OTMESS(all_pub_keys);
        MyUtil.sendOTMESS(oos, otmess_out);

        // Get sender's response - again in one message
        otmess_in = MyUtil.receiveOTMESS(ois);
        gr = otmess_in.num[2*v.size()]; // g^r - last num in mess

        // Perform all EG decryptions
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);
            selected = ottask.selected;

            byte[] valrec = MyUtil.EG_decrypt(gr,
                    otmess_in.num[2*i+selected], x[i]); // Decrypt
		    logger.debug("executeChooserOTs_EGNPBatch: OT no. " + i +
                " received: " + MyUtil.toHexString(valrec));
            ottask.setTransValue(valrec);
        }
    }

    //---------------------------------------------------------------

    /**
     * executeSenderOTs_EGNPBatch: El-Gamal OTs + Naor-Pinkas opt +
     *                             communication batching
     *
     * @param v Vector of OT task to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
 
    public void executeSenderOTs_EGNPBatch(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger r = MyUtil.EG_randExp(); // Generate random exponent r once
        BigInteger[] pub_key = new BigInteger[2];
        BigInteger[] all_encs = new BigInteger[2*v.size()+1];
        OTMESS otmess_out;
        OTTASK ottask;

        OTMESS otmess_in = MyUtil.receiveOTMESS(ois); // Get all public keys

        // Perform all EG encryptions
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);

            pub_key[0] = otmess_in.num[i];
            pub_key[1] = MyUtil.EG_deduce(pub_key[0]);

            byte[] mess0 = ottask.getElement(0);
            byte[] mess1 = ottask.getElement(1);
	    
            all_encs[2*i+0] = MyUtil.EG_encrypt(pub_key[0], r, mess0); // Encrypt mess0
            all_encs[2*i+1] = MyUtil.EG_encrypt(pub_key[1], r, mess1); // Encrypt mess1
            logger.debug("executeSenderOTs_EGNPBatch: OT no. " + i +
                " preparing: " + MyUtil.toHexString(mess0) + " or " +
                MyUtil.toHexString(mess1));
        }

        // Send all EG encryptions + g^r to chooser in one message
        all_encs[2*v.size()] = MyUtil.EG_pow(r); // g^r 
        otmess_out = new OTMESS(all_encs);
        MyUtil.sendOTMESS(oos, otmess_out);
    }

    //===============================================================

    /**
     * executeChooserOTs_PlainBatch: no real OT +
     *                               communication batching
     *
     * @param v Vector of OT tasks to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
    public void executeChooserOTs_PlainBatch(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger[] choices = new BigInteger[v.size()];

        // Gather all choices
        for (int i = 0; i < v.size(); i++) {
            OTTASK ottask = v.elementAt(i);
            choices[i] = BigInteger.valueOf(ottask.selected);
        }

        // Send all choices, get all values
        OTMESS otmess_out = new OTMESS(choices);
        MyUtil.sendOTMESS(oos, otmess_out);
        logger.debug("executeChooserOTs_PlainBatch: sent all choices");

        OTMESS otmess_in = MyUtil.receiveOTMESS(ois);
        logger.debug("executeChooserOTs_PlainBatch: received all values");

        // Set all values
        for (int i = 0; i < v.size(); i++) {
            OTTASK ottask = v.elementAt(i);
            byte[] valrec = otmess_in.num[i].toByteArray();
            ottask.setTransValue(valrec);
        }
    }

    //---------------------------------------------------------------

    /**
     * executeSenderOTs_PlainBatch: no real OT +
     *                              communication batching
     *
     * @param v Vector of OT task to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
    public void executeSenderOTs_PlainBatch(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        //BigInteger[] choices = new BigInteger[v.size()];
        BigInteger[] nums = new BigInteger[v.size()];

        // Get all choices
        OTMESS otmess_in = MyUtil.receiveOTMESS(ois);
        logger.debug("executeSenderOTs_PlainBatch: received all choices");

        // Gather all values corresponding to choices
        for (int i = 0; i < v.size(); i++) {
            OTTASK ottask = v.elementAt(i);
            int index = otmess_in.num[i].intValue();
            byte[] val2send = ottask.getElement(index);
            nums[i] = new BigInteger(val2send);
        }

        // Send all values
        OTMESS otmess_out = new OTMESS(nums);
        MyUtil.sendOTMESS(oos, otmess_out);
        logger.debug("executeSenderOTs_PlainBatch: sent all values");
    }

    //===============================================================

    /**
     * executeChooserOTs_EGOcomBatch: Only communication alla El-Gamal +
     *                                communication batching
     *
     * @param v Vector of OT tasks to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
    public void executeChooserOTs_EGOcomBatch(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger[] x = new BigInteger[v.size()];
        //BigInteger pub_key;
        BigInteger[] all_pub_keys = new BigInteger[v.size()];
        @SuppressWarnings("unused")
		OTMESS otmess_in;
        OTTASK ottask;
        @SuppressWarnings("unused")
		int selected;

        // Prepare all EG public keys and secret keys
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);
            selected = ottask.selected;
            x[i] = MyUtil.EG_g(); // Dummy operation
            all_pub_keys[i] = MyUtil.EG_g(); // Dummy operation
        }

        // Send all EG public keys to sender in one message
        OTMESS otmess_out = new OTMESS(all_pub_keys);
        MyUtil.sendOTMESS(oos, otmess_out);

        // Get sender's response - again in one message
        otmess_in = MyUtil.receiveOTMESS(ois);

        // Perform all EG decryptions
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);
            selected = ottask.selected;

            byte[] valrec = x[i].toByteArray(); // Dummy operation
            logger.debug("executeChooserOTs_EGOcomBatch: OT no. " + i +
                " received: " + MyUtil.toHexString(valrec));
            ottask.setTransValue(valrec);
        }
    }

    //---------------------------------------------------------------

    /**
     * executeSenderOTs_EGOcomBatch: Only communication alla El-Gamal+
     *                               communication batching
     *
     * @param v Vector of OT task to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
    public void executeSenderOTs_EGOcomBatch(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        @SuppressWarnings("unused")
		BigInteger r0;
        @SuppressWarnings("unused")
		BigInteger r1;
        BigInteger[] pub_key = new BigInteger[2];
        BigInteger[] all_encs = new BigInteger[4 * v.size()];
        OTMESS otmess_out;
        OTTASK ottask;

        OTMESS otmess_in = MyUtil.receiveOTMESS(ois); // Get all public keys

        // Perform all EG encryptions
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);

            pub_key[0] = otmess_in.num[i];
            pub_key[1] = MyUtil.EG_g(); // Dummy operation

            byte[] mess0 = ottask.getElement(0);
            byte[] mess1 = ottask.getElement(1);

            r0 = MyUtil.EG_g(); // Dummy operation
            r1 = MyUtil.EG_g(); // Dummy operation 

            all_encs[4*i+0] = MyUtil.EG_g(); // Dummy operation
            all_encs[4*i+1] = MyUtil.EG_g(); // Dummy operation
            all_encs[4*i+2] = MyUtil.EG_g(); // Dummy operation
            all_encs[4*i+3] = MyUtil.EG_g(); // Dummy operation

            logger.debug("executeSenderOTs_EGOcomBatch: OT no. " + i +
                " preparing: " + MyUtil.toHexString(mess0) + " or " +
                MyUtil.toHexString(mess1));
        }

        // Send all EG encryptions to chooser in one message
        otmess_out = new OTMESS(all_encs);
        MyUtil.sendOTMESS(oos, otmess_out);
    }

    //===============================================================

    /**
     * executeChooserOTs_EGNPOcomBatch: Only communication alla EG+NP+
     *                                  communication batching
     *
     * @param v Vector of OT tasks to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
 
    public void executeChooserOTs_EGNPOcomBatch(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger[] x = new BigInteger[v.size()];
        @SuppressWarnings("unused")
		BigInteger gr;
        BigInteger[] all_pub_keys = new BigInteger[v.size()];
        OTMESS otmess_in;
        OTTASK ottask;
        @SuppressWarnings("unused")
		int selected;

        // Prepare all EG public keys and secret keys
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);
            selected = ottask.selected;
            x[i] = MyUtil.EG_g(); // Dummy operation
            all_pub_keys[i] = MyUtil.EG_g(); // Dummy operation
        }

        // Send all EG public keys to sender in one message
        OTMESS otmess_out = new OTMESS(all_pub_keys);
        MyUtil.sendOTMESS(oos, otmess_out);

        // Get sender's response - again in one message
        otmess_in = MyUtil.receiveOTMESS(ois);
        gr = otmess_in.num[2*v.size()]; // g^r - last num in mess

        // Perform all EG decryptions
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);
            selected = ottask.selected;

            byte[] valrec = x[i].toByteArray(); // Dummy operation
            logger.debug("executeChooserOTs_EGNPOcomBatch: OT no. " + i +
                " received: " + MyUtil.toHexString(valrec));
            ottask.setTransValue(valrec);
        }
    }

    //---------------------------------------------------------------

    /**
     * executeSenderOTs_EGNPOcomBatch: Only communication alla EG+NP
     *                                 communication batching
     *
     * @param v Vector of OT task to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
 
    public void executeSenderOTs_EGNPOcomBatch(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        @SuppressWarnings("unused")
		BigInteger r = MyUtil.EG_g(); // Dummy operation
        BigInteger[] pub_key = new BigInteger[2];
        BigInteger[] all_encs = new BigInteger[2*v.size()+1];
        OTMESS otmess_out;
        OTTASK ottask;

        OTMESS otmess_in = MyUtil.receiveOTMESS(ois); // Get all public keys

        // Perform all EG encryptions
        for (int i = 0; i < v.size(); i++) {
            ottask = v.elementAt(i);

            pub_key[0] = otmess_in.num[i];
            pub_key[1] = MyUtil.EG_g(); // Dummy operation

            byte[] mess0 = ottask.getElement(0);
            byte[] mess1 = ottask.getElement(1);

            all_encs[2*i+0] = MyUtil.EG_g(); // Dummy operation
            all_encs[2*i+1] = MyUtil.EG_g(); // Dummy operation
            logger.debug("executeSenderOTs_EGNPOcomBatch: OT no. " + i +
                " preparing: " + MyUtil.toHexString(mess0) + " or " +
                MyUtil.toHexString(mess1));
        }

        // Send all EG encryptions + g^r to chooser in one message
        all_encs[2*v.size()] = MyUtil.EG_g(); // Dummy operation
        otmess_out = new OTMESS(all_encs);
        MyUtil.sendOTMESS(oos, otmess_out);
    }

    //===============================================================

    /**
     * executeChooserOTs_EG_NOC: El-Gamal OT + No Object Communication
     *
     * @param v Vector of OT tasks to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
    public void executeChooserOTs_EG_NOC(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
        BigInteger x;
        BigInteger pub_key;
        int selected;
		byte[] pub_key_ba = new byte[129];
		byte[] ciphertext = new byte[516];

        for (int i = 0; i < v.size(); i++) {
            OTTASK ottask = v.elementAt(i);
            selected = ottask.selected;
            x = MyUtil.EG_randExp(); // Generate random exponent x
            pub_key = MyUtil.EG_genPublic(selected, x); // Generate an EG pub key

		    MyUtil.BigInt2FixedBytes (pub_key, pub_key_ba);
		    MyUtil.sendBytes (oos, pub_key_ba, true);

		    MyUtil.receiveBytes (ois, ciphertext, 516); // Get sender's response
		    BigInteger[] nums = MyUtil.FixedBytes2BigInts (ciphertext, 258*selected, 2);

            //OTMESS otmess_in = MyUtil.receiveOTMESS(ois); // Get sender's response
            //byte[] valrec = MyUtil.EG_decrypt(otmess_in.num[2*selected+1],
                    //otmess_in.num[2*selected], x); // Decrypt
            byte[] valrec = MyUtil.EG_decrypt(nums[1], nums[0], x); // Decrypt

            logger.debug("executeChooserOTs_EG: OT no. " + i + " received: " +
                MyUtil.toHexString(valrec));
            ottask.setTransValue(valrec);
        }
    }

    //---------------------------------------------------------------

    /**
     * executeSenderOTs_EG_NOC: El-Gamal OT + No Object Communication
     *
     * @param v Vector of OT task to do
     * @param oos ObjectOutputStream to send messages to Sender
     * @param ois ObjectOutputStream to receive messages from Sender
     */
    public void executeSenderOTs_EG_NOC(Vector<OTTASK> v, ObjectOutputStream oos,
    	ObjectInputStream ois)
    {
        BigInteger r0;
        BigInteger r1;
        BigInteger[] num = new BigInteger[4];
        BigInteger[] pub_key = new BigInteger[2];
		byte[] pub_key_ba = new byte[129];
		byte[] ciphertext = new byte[516];

        for (int i = 0; i < v.size(); i++) {
            OTTASK ottask = v.elementAt(i);

		    MyUtil.receiveBytes (ois, pub_key_ba, 129);
	
		    pub_key[0] = MyUtil.FixedBytes2BigInt (pub_key_ba);
		    pub_key[1] = MyUtil.EG_deduce(pub_key[0]);

            r0 = MyUtil.EG_randExp(); // Generate random exponent r0
            r1 = MyUtil.EG_randExp(); // Generate random exponent r1

            byte[] mess0 = ottask.getElement(0);
            byte[] mess1 = ottask.getElement(1);
            num[0] = MyUtil.EG_encrypt(pub_key[0], r0, mess0); // Encrypt mess0
            num[1] = MyUtil.EG_pow(r0); // g^r0 
            num[2] = MyUtil.EG_encrypt(pub_key[1], r1, mess1); // Encrypt mess1
            num[3] = MyUtil.EG_pow(r1); // g^r1

            MyUtil.BigInts2FixedBytes (num, ciphertext);
            MyUtil.sendBytes (oos, ciphertext, true) ; // Send EG encryptions to chooser

            //OTMESS otmess_out = new OTMESS(num);
            //MyUtil.sendOTMESS(oos, otmess_out); // Send EG encryptions to chooser

            logger.debug("executeSenderOTs_EG: OT no. " + i + " sending: " +
                MyUtil.toHexString(mess0) + " or " + MyUtil.toHexString(mess1));
        }
    }

    public void executeChooserOTs_BOT(Vector<OTTASK> v, ObjectOutputStream oos,
    	ObjectInputStream ois)
    {
		@SuppressWarnings("unused")
		BigInteger blind, b;
		int selected;	
		
		for (int i = 0; i < v.size(); i++) {
		    OTTASK ottask = v.elementAt(i);
		    selected = ottask.selected;
		    OTMESS otmess_in = MyUtil.receiveOTMESS(ois); // Get sender's data
	
		    //Picking a random number
		    b = MyUtil.rand(MyUtil.key_size);
		    
		    //calculate SP(us[selected])*b^e
		    BigInteger sp = otmess_in.num[selected];
		    
		    BigInteger blindSign = (sp.multiply(MyUtil.encrypt(b))).mod(N);
		    OTMESS otmess_out = new OTMESS(blindSign);
	            MyUtil.sendOTMESS(oos, otmess_out);
		    
		    //Getting the results for final stage.
		    otmess_in = MyUtil.receiveOTMESS(ois);
		    
		    BigInteger unblind = ((otmess_in.num[0]).multiply(b.modInverse(N))).mod(N);
		    BigInteger k = new BigInteger(MyUtil.hash((unblind.toString()+""+selected).getBytes() , NBYTESG));
		    BigInteger valrec = otmess_in.num[selected+1].xor(k);
		    
		    //Done!
		    logger.debug("executeChooserOTs_BOT: OT no. " + i + " received: " +
				 MyUtil.toHexString(valrec.toByteArray() ));
	            ottask.setTransValue(valrec.toByteArray());
		}
    }
    
    public void executeSenderOTs_BOT(Vector<OTTASK> v, ObjectOutputStream oos,
    	ObjectInputStream ois)
    {
		BigInteger r;
		BigInteger[] us = new BigInteger[2];
		BigInteger[] ks = new BigInteger[2];
		BigInteger[] sp = new BigInteger[2];
		BigInteger[] yms = new BigInteger[3];
        OTMESS otmess_out;
        OTMESS otmess_in;

		for (int i = 0; i < v.size(); i++) {
		    OTTASK ottask = v.elementAt(i);
            byte[] mess0 = ottask.getElement(0);
		    byte[] mess1 = ottask.getElement(1);
		    //Picking two random numbers U0, U1 and padding them
		    us[0] = MyUtil.pad(MyUtil.rand((int)(MyUtil.key_size*0.75)));
		    us[1] = MyUtil.pad(MyUtil.rand((int)(MyUtil.key_size*0.75)));
		    
		    //calculate and save SP(us[0])^d , SP(us[1])^d
		    sp[0] = MyUtil.decrypt(us[0]);  
		    sp[1] = MyUtil.decrypt(us[1]);  
		    	    
		    //Sending to the chooser
		    otmess_out = new OTMESS(us);
		    MyUtil.sendOTMESS(oos, otmess_out); // Send everything to chooser
		    
		    //Getting the blind signature.
		    otmess_in = MyUtil.receiveOTMESS(ois);
		    //Picking random number R
		    r = MyUtil.rand(MyUtil.key_size);
		    //Sign and Blind y
		    BigInteger y = MyUtil.decrypt(otmess_in.num[0]);
		    y = (y.multiply(r)).mod(N);
		    //calculating the k-s
		    ks[0] = new BigInteger(MyUtil.hash((((sp[0].multiply(r)).mod(N)).toString()+"0").getBytes(), NBYTESG));
		    ks[1] = new BigInteger(MyUtil.hash((((sp[1].multiply(r)).mod(N)).toString()+"1").getBytes(), NBYTESG));
		    
		    //storing the y
		    yms[0] = y;
		    //encrypting the M-s
		    yms[1] = ks[0].xor(new BigInteger(mess0));
		    yms[2] = ks[1].xor(new BigInteger(mess1));
		    
		    //Sending the y and the encrypted stuff.
		    otmess_out = new OTMESS(yms);
		    MyUtil.sendOTMESS(oos, otmess_out); // Send everything to chooser	
		    logger.debug("executeSenderOTs_BOT: OT no. " + i + " sending: " +
				 MyUtil.toHexString(mess0) + " or " + MyUtil.toHexString(mess1));
		}
    }
    
    public void executeChooserOTs_BOTBatch(Vector<OTTASK> v, ObjectOutputStream oos,
    	ObjectInputStream ois)
    {
		@SuppressWarnings("unused")
		BigInteger blind;
		BigInteger[] b = new BigInteger[v.size()];
		BigInteger[] blindSign = new BigInteger[v.size()];
		int[] selected = new int[v.size()];	
		OTTASK ottask;
	
		OTMESS otmess_in = MyUtil.receiveOTMESS(ois); // Get sender's data
		for (int i = 0; i < v.size(); i++) {
		    ottask = v.elementAt(i);
		    selected[i] = ottask.selected;
		    //Picking a random number
		    b[i] = MyUtil.rand(MyUtil.key_size);
		    //calculate SP(us[selected])*b^e
		    BigInteger sp = otmess_in.num[2*i+selected[i]];
		    blindSign[i] = (sp.multiply(MyUtil.encrypt(b[i]))).mod(N);
		}
		
		OTMESS otmess_out = new OTMESS(blindSign);
		MyUtil.sendOTMESS(oos, otmess_out);
		
		//Getting the results for final stage.
		otmess_in = MyUtil.receiveOTMESS(ois);
		
		for (int i = 0; i < v.size(); i++) {
		    BigInteger unblind = ((otmess_in.num[3*i]).multiply(b[i].modInverse(N))).mod(N);
		    BigInteger k = new BigInteger(MyUtil.hash((unblind.toString()+""+selected[i]).getBytes() , NBYTESG));
		    BigInteger valrec = otmess_in.num[3*i+1+selected[i]].xor(k);
		    
		    logger.debug("executeChooserOTs_BOT: OT no. " + i + " received: " +
				 MyUtil.toHexString(valrec.toByteArray() ));
		    ottask = v.elementAt(i);
            ottask.setTransValue(valrec.toByteArray());
		}
    }

    public void executeSenderOTs_BOTBatch(Vector<OTTASK> v, ObjectOutputStream oos,
        ObjectInputStream ois)
    {
		BigInteger r,y;
		BigInteger[] us = new BigInteger[2*v.size()];
		BigInteger[] ks = new BigInteger[2*v.size()];
		BigInteger[] sp = new BigInteger[2*v.size()];
		BigInteger[] yms = new BigInteger[3*v.size()];
		BigInteger[] mess = new BigInteger[2*v.size()];
		OTMESS otmess_out;
		OTMESS otmess_in;
		OTTASK ottask;
		for (int i = 0; i < v.size(); i++) {
		    //Picking two random numbers U0, U1 and padding them
		    us[2*i] = MyUtil.pad(MyUtil.rand((int)(MyUtil.key_size*0.75)));
		    us[2*i+1] = MyUtil.pad(MyUtil.rand((int)(MyUtil.key_size*0.75)));
		    
	
		    ottask = v.elementAt(i);
	        byte[] mess0 = ottask.getElement(0);
		    byte[] mess1 = ottask.getElement(1);
		    mess[2*i] = new BigInteger(mess0);
		    mess[2*i+1] = new BigInteger(mess1);
		    //calculate and save SP(us[0])^d , SP(us[1])^d
		    sp[2*i] = MyUtil.decrypt(us[2*i]);  
		    sp[2*i+1] = MyUtil.decrypt(us[2*i+1]);
		}
		//Sending to the chooser
		otmess_out = new OTMESS(us);
		MyUtil.sendOTMESS(oos, otmess_out); // Send everything to chooser
		//Getting the blind signatures.
		otmess_in = MyUtil.receiveOTMESS(ois);
		for (int i = 0; i < v.size(); i++) {
		    r = MyUtil.rand(MyUtil.key_size);
		    //Sign and Blind y
		    y = MyUtil.decrypt(otmess_in.num[i]);
		    
		    y = (y.multiply(r)).mod(N);
		    //calculating the k-s
		    ks[2*i] = new BigInteger(MyUtil.hash((((sp[2*i].multiply(r)).mod(N)).toString()+"0").getBytes(), NBYTESG));
		    ks[2*i+1] = new BigInteger(MyUtil.hash((((sp[2*i+1].multiply(r)).mod(N)).toString()+"1").getBytes(), NBYTESG));
	
		    //storing the y
		    yms[3*i] = y;
		    //encrypting the M-s
		    yms[3*i+1] = ks[2*i].xor(mess[2*i]);
		    yms[3*i+2] = ks[2*i+1].xor(mess[2*i+1]);
		    logger.debug("executeSenderOTs_BOT: OT no. " + i + " sending: " +
				 MyUtil.toHexString(mess[2*i].toByteArray()) + " or " + MyUtil.toHexString(mess[2*i+1].toByteArray()));
		}
		//Sending the y and the encrypted stuff.
		otmess_out = new OTMESS(yms);
		MyUtil.sendOTMESS(oos, otmess_out); // Send everything to chooser
    }
}
