package com.ninja.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class TheLedgerCo {
    public static void main(String[] args) throws FileNotFoundException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the file path");
        String filePath = sc.next();
        File file = new File(filePath);
//        File file = new File("/Users/DeveshR/Desktop/ledger.txt");
        Scanner inputFile = new Scanner(file);
        StringBuilder output = new StringBuilder();
        Map<LedgerKey, LedgerDto> map = new HashMap<>();
        while (inputFile.hasNextLine()) {
            String record = inputFile.nextLine();
            System.out.println(record);
            String[] tuples = record.split("\\s+");
            String command = tuples[0];
            if (command.equalsIgnoreCase(Commands.LOAN.name())) {
                LedgerDto ledgerDto = LedgerDto.builder()
                        .bankName(tuples[1])
                        .borrowerName(tuples[2])
                        .p(Integer.parseInt(tuples[3]))
                        .n(Integer.parseInt(tuples[4]))
                        .r(Integer.parseInt(tuples[5]))
                        .build();
                int p = Integer.parseInt(tuples[3]);
                int n = Integer.parseInt(tuples[4]);
                int r = Integer.parseInt(tuples[5]);
                int interest = (int) Math.ceil(p * n * ((double)r / 100));
                int totalAmount = p + interest;
                ledgerDto.setTotalAmount(totalAmount);
                int emi = (int) Math.ceil(totalAmount / ((double)n * 12));
                ledgerDto.setEmi(emi);
                ledgerDto.setLastMonthEmiAmount(totalAmount - emi * ((n * 12) - 1));
                ledgerDto.setEmiTotalMonths(n * 12);
                ledgerDto.setEmiRevisedTotalMonths(n * 12);
                LedgerKey ledgerKey = LedgerKey.builder().bankName(ledgerDto.getBankName())
                        .borrowerName(ledgerDto.getBorrowerName()).build();
                map.putIfAbsent(ledgerKey, ledgerDto);
            } else if (command.equalsIgnoreCase(Commands.PAYMENT.name())) {
                String bankName = tuples[1];
                String borrowerName = tuples[2];
                int lumpSumAmount = Integer.parseInt(tuples[3]);
                int lumpSumAmountEmiNo = Integer.parseInt(tuples[4]);
                LedgerKey ledgerKey = LedgerKey.builder().bankName(bankName).borrowerName(borrowerName).build();
                LedgerDto ledgerDto = map.get(ledgerKey);
                ledgerDto.setLumpSumAmount(lumpSumAmount);
                ledgerDto.setLumpSumAmountEmiNo(lumpSumAmountEmiNo);
                int totalAmount = ledgerDto.getTotalAmount();
                int emi = ledgerDto.getEmi();
                int amountPaidIncludingLsa = (emi * lumpSumAmountEmiNo) + lumpSumAmount;
                int remainingAmountToBePaid = totalAmount - amountPaidIncludingLsa;
                int emiRevisedTotalMonths = lumpSumAmountEmiNo + (int) Math.ceil(remainingAmountToBePaid / emi);
                ledgerDto.setLastMonthEmiAmount(((remainingAmountToBePaid % emi) == 0) ? emi : remainingAmountToBePaid % emi);
                ledgerDto.setEmiRevisedTotalMonths(emiRevisedTotalMonths);
            } else if (command.equalsIgnoreCase(Commands.BALANCE.name())) {
                String bankName = tuples[1];
                String borrowerName = tuples[2];
                int inputEmiNo = Integer.parseInt(tuples[3]);
                LedgerKey ledgerKey = LedgerKey.builder().bankName(bankName).borrowerName(borrowerName).build();
                LedgerDto ledgerDto = map.get(ledgerKey);
                if (Objects.isNull(ledgerDto.getLumpSumAmount())) {
                    if (inputEmiNo >= ledgerDto.getEmiTotalMonths()) { //Last/Exceeded emi number case
                        output.append(bankName).append(" ").append(borrowerName).append(" ").append(ledgerDto.totalAmount).append(" ").append(0).append("\n");
                    } else {
                        output.append(bankName).append(" ").append(borrowerName).append(" ").append(ledgerDto.emi * inputEmiNo).append(" ").append(ledgerDto.getEmiTotalMonths() - inputEmiNo).append("\n");
                    }
                } else { //LumpSumPayment done case
                    if (inputEmiNo >= ledgerDto.getEmiRevisedTotalMonths()) { //Last/Exceeded emi number case
                        output.append(bankName).append(" ").append(borrowerName).append(" ").append(ledgerDto.totalAmount).append(" ").append(0).append("\n");
                    } else if (inputEmiNo >= ledgerDto.getLumpSumAmountEmiNo()) {
                        int amountPaid = (ledgerDto.getEmi() * inputEmiNo) + ledgerDto.getLumpSumAmount();
                        output.append(bankName).append(" ").append(borrowerName).append(" ").append(amountPaid).append(" ").append(ledgerDto.getEmiRevisedTotalMonths() - inputEmiNo + 1).append("\n");
                    } else {
                        output.append(bankName).append(" ").append(borrowerName).append(" ").append(ledgerDto.emi * inputEmiNo).append(" ").append(ledgerDto.getEmiTotalMonths() - inputEmiNo).append("\n");
                    }
                }
            }
        }

        //print output
        System.out.println(output);
//        System.out.println(map);
    }

    private enum Commands {
        LOAN, PAYMENT, BALANCE
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class LedgerDto {
        private int emi; //amount to be paid per month
        private String bankName;
        private String borrowerName;
        private int p, n, r;
        private int interest;
        private int totalAmount;
        private Integer lumpSumAmount;
        private Integer lumpSumAmountEmiNo;
        private int emiTotalMonths;
        private Integer emiRevisedTotalMonths;
        private int lastMonthEmiAmount;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class LedgerKey {
        private String bankName;
        private String borrowerName;
    }
}
