package com.c2c.wallet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.c2c.common.exception.BusinessException;
import com.c2c.wallet.entity.BankCard;
import com.c2c.wallet.mapper.BankCardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 演示银行卡服务：绑卡 / 解绑 / 设默认 / 我的卡列表。
 * <p>未接入真实支付，仅课程演示；充值(银行卡方式)与提现均基于本模拟卡。</p>
 */
@Service
@RequiredArgsConstructor
public class BankCardService {

    private final BankCardMapper bankCardMapper;

    /** 演示卡号：8~30 位数字（模拟校验，不校验真实发卡行） */
    private static final Pattern CARD_NO_PATTERN = Pattern.compile("^\\d{8,30}$");

    /** 我的卡列表：默认卡优先，其余按 ID 倒序 */
    public List<BankCard> list(Long userId) {
        return bankCardMapper.selectList(new LambdaQueryWrapper<BankCard>()
                .eq(BankCard::getUserId, userId)
                .orderByDesc(BankCard::getIsDefault)
                .orderByDesc(BankCard::getId));
    }

    /** 绑定演示卡；首张自动设为默认 */
    @Transactional
    public BankCard add(Long userId, String holder, String cardNo, String bankName) {
        if (holder == null || holder.trim().isEmpty()) {
            throw new BusinessException("持卡人姓名不能为空");
        }
        if (cardNo == null || !CARD_NO_PATTERN.matcher(cardNo.trim()).matches()) {
            throw new BusinessException("请输入 8~30 位数字的演示卡号");
        }
        long count = bankCardMapper.selectCount(new LambdaQueryWrapper<BankCard>()
                .eq(BankCard::getUserId, userId)
                .eq(BankCard::getCardNo, cardNo.trim()));
        if (count > 0) {
            throw new BusinessException("该银行卡已绑定");
        }
        boolean first = bankCardMapper.selectCount(
                new LambdaQueryWrapper<BankCard>().eq(BankCard::getUserId, userId)) == 0;

        BankCard card = new BankCard();
        card.setUserId(userId);
        card.setCardHolder(holder.trim());
        card.setCardNo(cardNo.trim());
        card.setBankName(bankName == null ? null : bankName.trim());
        card.setIsDefault(first ? 1 : 0);
        bankCardMapper.insert(card);
        return card;
    }

    /** 解绑；若移除的是默认卡，把剩余最早一张补为默认 */
    @Transactional
    public void remove(Long userId, Long cardId) {
        BankCard card = requireOwned(userId, cardId);
        bankCardMapper.deleteById(card.getId());
        if (Integer.valueOf(1).equals(card.getIsDefault())) {
            BankCard next = bankCardMapper.selectOne(new LambdaQueryWrapper<BankCard>()
                    .eq(BankCard::getUserId, userId)
                    .orderByAsc(BankCard::getId)
                    .last("LIMIT 1"));
            if (next != null) {
                bankCardMapper.update(null, new UpdateWrapper<BankCard>()
                        .eq("id", next.getId()).set("is_default", 1));
            }
        }
    }

    /** 设为默认卡：先清本人其余默认，再置当前卡 */
    @Transactional
    public void setDefault(Long userId, Long cardId) {
        requireOwned(userId, cardId);
        bankCardMapper.update(null, new UpdateWrapper<BankCard>()
                .eq("user_id", userId).set("is_default", 0));
        bankCardMapper.update(null, new UpdateWrapper<BankCard>()
                .eq("id", cardId).set("is_default", 1));
    }

    /** 取默认卡；无默认卡时返回本人最早一张 */
    public BankCard getDefaultCard(Long userId) {
        BankCard def = bankCardMapper.selectOne(new LambdaQueryWrapper<BankCard>()
                .eq(BankCard::getUserId, userId)
                .eq(BankCard::getIsDefault, 1)
                .last("LIMIT 1"));
        if (def != null) {
            return def;
        }
        return bankCardMapper.selectOne(new LambdaQueryWrapper<BankCard>()
                .eq(BankCard::getUserId, userId)
                .orderByAsc(BankCard::getId)
                .last("LIMIT 1"));
    }

    /** 充值/提现前校验已绑卡，未绑抛业务提示 */
    public BankCard requireBoundCard(Long userId) {
        BankCard card = getDefaultCard(userId);
        if (card == null) {
            throw new BusinessException("请先绑定演示银行卡");
        }
        return card;
    }

    private BankCard requireOwned(Long userId, Long cardId) {
        BankCard card = bankCardMapper.selectById(cardId);
        if (card == null || !card.getUserId().equals(userId)) {
            throw new BusinessException("银行卡不存在");
        }
        return card;
    }
}
