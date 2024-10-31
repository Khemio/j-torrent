import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

public class Main {
  private static final Gson gson = new Gson();
  

  public static void main(String[] args) throws Exception {
    String command = args[0];
    
    if("decode".equals(command)) {
      String bencodedValue = args[1];
      String decoded;

      switch (bencodedValue.charAt(0)) {
        case 'i' -> {
          Bencode bencode = new Bencode(true);
          decoded = String.valueOf(bencode.decode(bencodedValue.getBytes(), Type.NUMBER));
        }

        case 'l' -> {
          Bencode bencode = new Bencode(false);
          decoded = gson.toJson(bencode.decode(bencodedValue.getBytes(), Type.LIST));
        }

        case 'd'-> {
          Bencode bencode = new Bencode(false);
          decoded = gson.toJson(bencode.decode(bencodedValue.getBytes(), Type.DICTIONARY));
        }

      
        default -> {
          try {
            decoded = gson.toJson(decodeBencode(bencodedValue));
          } catch(RuntimeException e) {
            System.out.println(e.getMessage());
            return;
          }
        }
      }


      System.out.println(decoded);

    } else if ("info".equals(command)) {
      Torrent torrent = new Torrent(Files.readAllBytes(Path.of(args[1])));

      System.out.println("Tracker URL: " + torrent.announce);
      System.out.println("Length: " + torrent.length);
      System.out.println("Info Hash: " + torrent.hash);
      System.out.println("Piece Length: " + torrent.pieceLength);
      System.out.println("Piece Hashes: ");
      
      for (String piece : torrent.pieces) {
        System.out.println(piece);
      }

    } else {
      System.out.println("Unknown command: " + command);
    }
  }

  static String decodeBencode(String bencodedString) {
    if (Character.isDigit(bencodedString.charAt(0))) {
      int firstColonIndex = 0;

      for(int i = 0; i < bencodedString.length(); i++) { 
        if(bencodedString.charAt(i) == ':') {
          firstColonIndex = i;
          break;

        }
      }

      int length = Integer.parseInt(bencodedString.substring(0, firstColonIndex));

      return bencodedString.substring(firstColonIndex+1, firstColonIndex+1+length);
    } else {
      throw new RuntimeException("Only strings are supported at the moment");
    }
  }

  static class Torrent {
    public String announce;
    public long length;
    public String hash;
    public long pieceLength;
    public String[] pieces;

    
    Torrent(byte[] bytes) {
      Bencode bencode = new Bencode(false);
      Bencode bencodeRaw = new Bencode(true);

      try {
        Map<String, Object> root = bencode.decode(bytes, Type.DICTIONARY);
        Map<String, Object> info = (Map<String, Object>) root.get("info");
        
        Map<String, ?> infoRaw = (Map<String, ?>) bencodeRaw.decode(bytes, Type.DICTIONARY).get("info");
        byte[] bencodedInfo = bencodeRaw.encode(infoRaw);
        hash = sha1(bencodedInfo);

        announce = String.valueOf(root.get("announce"));
        length = Long.parseLong(String.valueOf(info.get("length")));
        pieceLength = Long.parseLong(String.valueOf(info.get("piece length")));

        ByteBuffer pieceBuffer = (ByteBuffer) infoRaw.get("pieces");
        byte[] piecesRaw = new byte[pieceBuffer.remaining()];
        pieceBuffer.get(piecesRaw);
        
        int numOfPieces = piecesRaw.length / 20;

        pieces = new String[numOfPieces];

        StringBuilder sb = new StringBuilder(20);
        for (int i = 0; i < numOfPieces ; i++) {

          int start = i * 20;
          int end = i * 20 + 20;

          for (int j = start; j < end; j++) {
            sb.append(String.format("%02x", piecesRaw[j]));
          }

          pieces[i] = sb.toString();
          sb.setLength(0);
        
        }

      } catch(RuntimeException e) {
        System.out.println(e.getMessage());
        return;
      } catch(NoSuchAlgorithmException e) {
        System.out.println(e.getMessage());
      }
    }

    String sha1(byte[] bytes) throws NoSuchAlgorithmException {
      MessageDigest md = MessageDigest.getInstance("SHA1");
      byte[] result = md.digest(bytes);

      StringBuilder sb = new StringBuilder();
      // BigInteger no = new BigInteger(1, result);
      // String hash = no.toString(16);

      // while (hash.length() < 40) {
      //   hash = "0" + hash;
      // }

      for (int i = 0; i < result.length; i++) {
        sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
      }
     
      return sb.toString();

      // return hash;
    }
  }

}
